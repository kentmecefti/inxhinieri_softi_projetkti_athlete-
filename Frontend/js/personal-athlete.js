
const API = window.env?.API || "http://localhost:8080/api";
const ATHLETE_ID = new URLSearchParams(location.search).get("athleteId") || 1;

document.addEventListener("DOMContentLoaded", () => {
  handleBackButton();
  loadAthlete();
  setupMainTabs();
  hideAllFrames(); 
});

function getAuthHeaders() {
  const token = localStorage.getItem("token");
  if (!token) {
    showToast("⚠️ You are not logged in. Please log in again.", "error");
    window.location.href = "../login.html";
    return {};
  }
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json"
  };
}

function handleBackButton() {
  const backBtn = document.querySelector(".top-actions button");
  const storedAthleteId = localStorage.getItem("athleteId");

  if (!backBtn) return;

  backBtn.style.display = storedAthleteId ? "none" : "inline-flex";
}

async function loadAthlete() {
  try {
    const res = await fetch(`${API}/athletes/${ATHLETE_ID}`, {
      headers: getAuthHeaders()
    });
    if (!res.ok) throw new Error("Failed to load athlete");

    const athlete = await res.json();

    if (isAdminUser()) {
      const titleEl = document.getElementById("pageTitle");
      if (titleEl) {
        titleEl.textContent = `🏃 Personal Athlete Page — ${athlete.name}`;
      }
    }

    await loadCoaches();
  } catch (err) {
    console.error(err);
    showToast("⚠️ Failed to load athlete", "error");
  }
}

async function loadCoaches() {
  try {
    const res = await fetch(
        `${API}/athletes/${ATHLETE_ID}/coaches/decision/accept`,
        { headers: getAuthHeaders() }
    );

    if (!res.ok) throw new Error("Failed to load coaches");

    renderCoaches(await res.json());
  } catch (err) {
    console.error(err);
    showToast("⚠️ Failed to load coaches", "error");
  }
}

function renderCoaches(coaches) {
  const container = document.getElementById("coachList");
  if (!container) return;

  if (!coaches.length) {
    container.innerHTML = "<p><em>No accepted coaches yet.</em></p>";
    return;
  }

  container.innerHTML = `
    <p><strong>Accepted Coaches:</strong></p>
    ${coaches.map(c => `
      <div class="coach-item">
        <span class="coach-name">${c.name} ${c.lastname || ""}</span>
        <button class="remove-link" onclick="removeCoachLink(${c.coach_id})">❌ Remove</button>
      </div>
    `).join("")}
  `;
}

async function removeCoachLink(coachId) {
  if (!confirm("Are you sure you want to unlink this coach?")) return;

  try {
    const res = await fetch(
        `${API}/coaches/${coachId}/athletes/${ATHLETE_ID}`,
        { method: "DELETE", headers: getAuthHeaders() }
    );

    if (!res.ok) throw new Error("Failed to unlink coach");

    showToast("✅ Coach unlinked successfully");
    loadCoaches();
  } catch (err) {
    console.error(err);
    showToast("⚠️ Failed to unlink coach", "error");
  }
}

function openDivision(id, name, type) {
  hideAllFrames();

  const frame = document.getElementById("resultFrame");
  const section = document.getElementById("resultFrameSection");
  if (!frame || !section) return;

  const map = {
    runner: "new-runner",
    jumper: "jumper",
    throw: "throw",
    gym: "gym",
    marathon: "marathon"
  };

  frame.src = `./pages/work/${map[type]}.html?id=${id}`;
  section.style.display = "block";
  document.body.classList.add("iframe-open");
}

function openPlan(athleteId) {
  hideAllFrames();

  const frame = document.getElementById("planFrame");
  const section = document.getElementById("planFrameSection");
  if (!frame || !section) return;

  frame.src = `./pages/plan.html?athleteId=${athleteId}`;
  section.style.display = "block";
  document.body.classList.add("iframe-open");
}

function openRequests() {
  hideAllFrames();

  const frame = document.getElementById("requestFrame");
  const section = document.getElementById("requestFrameSection");
  if (!frame || !section) return;

  frame.src = `./pages/requests.html?athleteId=${ATHLETE_ID}&t=${Date.now()}`;
  section.style.display = "block";
  document.body.classList.add("iframe-open");
}

function openInfo(athleteId) {
  hideAllFrames();

  const frame = document.getElementById("infoFrame");
  const section = document.getElementById("infoFrameSection");
  if (!frame || !section) return;

  frame.src = `./pages/athlete-info.html?athleteId=${athleteId}`;
  section.style.display = "block";
  document.body.classList.add("iframe-open");
}

function hideAllFrames() {
  document.querySelectorAll(".iframe-section").forEach(sec => {
    sec.style.display = "none";
  });

  document.body.classList.remove("iframe-open");
}

function setupMainTabs() {
  const tabs = document.querySelectorAll(".tab");
  const sections = document.querySelectorAll(".tab-content");

  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      tabs.forEach(t => t.classList.remove("active"));
      tab.classList.add("active");

      const target = tab.dataset.target;
      sections.forEach(sec => sec.classList.add("hidden"));
      document.getElementById(target)?.classList.remove("hidden");

      hideAllFrames();

      if (target === "infoTab") {
        openInfo(ATHLETE_ID);
      }
    });
  });
}

function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.className = "toast show";
  toast.textContent = message;
  document.body.appendChild(toast);

  setTimeout(() => toast.remove(), 2500);
}

window.addEventListener("message", e => {
  if (e.data === "refreshCoaches") {
    loadCoaches();
    showToast("✅ Coach list updated");
  }
  if (e.data === "closeIframe") {
    hideAllFrames();
  }
});

function goBack() {
  window.location.href = "all-users.html";
}

function isAdminUser() {
  const roles = JSON.parse(localStorage.getItem("userRoles") || "[]");
  return roles.includes("ADMIN") || roles.includes("ROLE_ADMIN");
}
