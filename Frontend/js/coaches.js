
const API = window.env?.API || "http://localhost:8080/api";
const params = new URLSearchParams(window.location.search);
const coachId = params.get("coachId");
let currentCoachId = coachId || null;

document.addEventListener("DOMContentLoaded", () => {

  setupTabs();

  const athleteFrame = document.getElementById("athleteFrame");
  if (athleteFrame) {
    athleteFrame.src = coachId
      ? `./pages/athlete.html?coachId=${coachId}`
      : `./pages/athlete.html`;
  }

  const storedCoachId = localStorage.getItem("coachId");
  const backBtn = document.querySelector(".back-btn");
  if (backBtn) {
    backBtn.style.display = storedCoachId ? "none" : "inline-flex";
  }

  if (coachId) {
    currentCoachId = coachId;
    document.querySelector(".split")?.classList.remove("hidden");
    document.getElementById("coachPage")?.classList.add("hidden");
    loadCoach();
  } else {
    document.querySelector(".split")?.classList.add("hidden");
    document.getElementById("coachPage")?.classList.remove("hidden");
    loadCoaches();
  }
});

function getAuthHeaders() {
  const token = localStorage.getItem("token");
  if (!token) {
    alert("Session expired. Please log in again.");
    window.location.href = "../login.html";
    return {};
  }
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json"
  };
}

async function loadCoach() {
  try {
    const res = await fetch(`${API}/coaches/${coachId}`, {
      headers: getAuthHeaders()
    });
    if (!res.ok) throw new Error("Failed to load coach");

    const plansRes = await fetch(`${API}/coaches/${coachId}/plans`, {
      headers: getAuthHeaders()
    });
    const plans = plansRes.ok ? await plansRes.json() : [];

    const planList = document.getElementById("planList");
    if (planList) {
      planList.innerHTML = plans.length
        ? plans.map(p => `
            <button class="plan-item" onclick="goToPlan(${p.id})">
              ${p.title || "Untitled Plan"}
            </button>
          `).join("")
        : `<p class="muted">No plans yet.</p>`;
    }

  } catch (err) {
    console.error(err);
    showToast("Error loading coach data", "error");
  }
}

async function loadCoaches() {
  try {
    const res = await fetch(`${API}/coaches`, {
      headers: getAuthHeaders()
    });
    if (!res.ok) throw new Error("Failed to fetch coaches");

    const coaches = await res.json();
    const container = document.getElementById("coachList");
    if (!container) return;

    container.innerHTML = "";

    coaches.forEach(coach => {
      const card = document.createElement("div");
      card.className = "card";
      card.innerHTML = `
        <h3>${coach.name}</h3>
        <p>ID: ${coach.id}</p>
        <div class="actions">
          <button class="view-btn">👀 View Dashboard</button>
        </div>
      `;
      card.querySelector(".view-btn").onclick = () =>
        window.location.href = `personal-coach.html?coachId=${coach.id}`;
      container.appendChild(card);
    });

  } catch (err) {
    console.error(err);
    showToast("Error loading coaches", "error");
  }
}

function reloadIframe(iframe) {
  if (!iframe || !iframe.src) return;
  const src = iframe.src;
  iframe.src = "";
  setTimeout(() => iframe.src = src, 50);
}

function setupTabs() {
  const tabs = document.querySelectorAll(".tab");
  const contents = document.querySelectorAll(".tab-content");

  if (!tabs.length || !contents.length) return;

  const ATHLETE_LABEL = "Athletes";
  const ATHLETE_BACK_LABEL = "← Back to Athlete";

  let activeTab = null;

  contents.forEach(c => c.classList.add("hidden"));

  tabs.forEach(tab => {

    tab.addEventListener("click", () => {
      const target = tab.dataset.target;

      if (activeTab === tab && target === "athletesTab") {
        const athleteFrame = document.getElementById("athleteFrame");
        if (athleteFrame) {
          athleteFrame.src = coachId
            ? `./pages/athlete.html?coachId=${coachId}&reset=${Date.now()}`
            : `./pages/athlete.html?reset=${Date.now()}`;
        }
        return;
      }

      tabs.forEach(t => {
        t.classList.remove("active");
        if (t.dataset.target === "athletesTab") {
          t.textContent = ATHLETE_LABEL;
        }
      });

      contents.forEach(c => {
        c.classList.remove("active");
        c.classList.add("hidden");
      });

      activeTab = tab;
      tab.classList.add("active");

      if (target === "athletesTab") {
        tab.textContent = ATHLETE_BACK_LABEL;
      }

      const section = document.getElementById(target);
      if (section) {
        section.classList.remove("hidden");
        section.classList.add("active");
      }

      if (target === "plansTab") {
        const planFrame = document.getElementById("planFrame");
        if (planFrame && !planFrame.src) {
          planFrame.src = coachId
            ? `./pages/plan.html?coachId=${coachId}`
            : `./pages/plan.html`;
        }
      }

      if (target === "coachInfoTab") {
        const infoFrame = document.getElementById("coachInfoFrame");
        if (infoFrame && !infoFrame.src) {
          infoFrame.src = coachId
            ? `./pages/coach-info.html?coachId=${coachId}`
            : `./pages/coach-info.html`;
        }
      }
    });

    tab.addEventListener("dblclick", () => {
      const target = tab.dataset.target;

      if (target === "plansTab") {
        reloadIframe(document.getElementById("planFrame"));
        showToast("Plans reloaded");
      }

      if (target === "coachInfoTab") {
        reloadIframe(document.getElementById("coachInfoFrame"));
        showToast("Coach info reloaded");
      }

      if (target === "athletesTab") {
        reloadIframe(document.getElementById("athleteFrame"));
        showToast("Athletes reloaded");
      }
    });
  });
}

function goToAthletes() {
  window.location.href = `./pages/athlete.html?coachId=${coachId}`;
}

function goToPlan(id) {
  window.location.href = `./pages/plan.html?planId=${id}`;
}

function goToPlanPage() {
  window.location.href = `./pages/plan.html?coachId=${currentCoachId || coachId}`;
}

function goBack() {
  window.location.href = "all-users.html";
}

function showToast(msg, type = "info") {
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.textContent = msg;

  if (type === "error") toast.style.background = "#e74c3c";

  document.body.appendChild(toast);
  setTimeout(() => toast.classList.add("show"), 100);
  setTimeout(() => {
    toast.classList.remove("show");
    toast.remove();
  }, 2200);
}

window.goToAthletes = goToAthletes;
window.goToPlanPage = goToPlanPage;
window.goBack = goBack;
window.goToPlan = goToPlan;
