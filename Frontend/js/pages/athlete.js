
const API = window.env?.API || "http://localhost:8080/api";
let athletes = [];
const params = new URLSearchParams(location.search);
const coachIdFromUrl = params.get("coachId");

(function guardAccess() {
  const token = localStorage.getItem("token");
  const roles = JSON.parse(localStorage.getItem("userRoles") || "[]");

  const isCoach = roles.includes("COACH") || roles.includes("ROLE_COACH");
  const isAdmin = roles.includes("ADMIN") || roles.includes("ROLE_ADMIN");

  if (!token || (!isCoach && !isAdmin)) {
    document.querySelector(".container").innerHTML = "";

    const msg = document.getElementById("accessMessage");
    msg.style.display = "block";
    msg.innerHTML = `
      <div style="
        max-width: 500px;
        margin: 80px auto;
        padding: 30px;
        text-align: center;
        background: #fff;
        border-radius: 14px;
        box-shadow: 0 10px 30px rgba(0,0,0,0.15);
        font-family: Segoe UI, sans-serif;
      ">
        <h2 style="color:#e74c3c;">🚫 Access denied</h2>
        <p style="margin-top:10px; color:#555;">
          You are not authorized to view this page.
        </p>
        <p style="color:#777; font-size:14px;">
          Please log in as a <b>Coach</b>.
        </p>
        <button onclick="window.location.href='../../login.html'"
          style="
            margin-top:20px;
            padding:10px 18px;
            border:none;
            border-radius:8px;
            background:#2563eb;
            color:#fff;
            font-weight:600;
            cursor:pointer;
          ">
          🔐 Go to Login
        </button>
      </div>
    `;

    throw new Error("Access denied");
  }
})();

function getAuthHeaders() {
  const token = localStorage.getItem("token");
  if (!token) {
    alert("You are not logged in. Please log in again.");
    window.location.href = "../../login.html";
    return {};
  }
  return {
    "Authorization": `Bearer ${token}`,
    "Content-Type": "application/json"
  };
}

async function getCurrentCoachId() {
  if (coachIdFromUrl) return coachIdFromUrl;

  const cached = localStorage.getItem("coachId");
  if (cached) return cached;

  try {
    const res = await fetch(`${API}/coaches/me`, { headers: getAuthHeaders() });
    if (!res.ok) throw new Error("Could not fetch /coaches/me");
    const coach = await res.json();
    localStorage.setItem("coachId", coach.id);
    return coach.id;
  } catch (e) {
    console.error("⚠️ Failed to resolve coachId:", e);
    showToast("⚠️ Cannot resolve your coach ID (auth?).", "error");
    return null;
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  console.log("✅ API:", API);
  console.log("🎓 Coach ID (URL):", coachIdFromUrl);

  const cid = await getCurrentCoachId();
  if (!cid) return;

  loadCoach(cid);
  loadAthletesByStatus("accept");

  const container = document.querySelector(".container");
  const filterBar = document.createElement("div");
  filterBar.className = "status-filters";
  filterBar.innerHTML = `
    <div style="text-align:center; margin-bottom:20px;">
      <button id="filterAccept" class="filter-btn" style="background:#2d9f25;color:#fff;border:none;padding:6px 12px;border-radius:6px;margin:0 4px;">✅ Accepted</button>
      <button id="filterPending" class="filter-btn" style="background:#2563eb;color:#fff;border:none;padding:6px 12px;border-radius:6px;margin:0 4px;">⏳ Pending</button>
      <button id="filterRefuse" class="filter-btn" style="background:#e74c3c;color:#fff;border:none;padding:6px 12px;border-radius:6px;margin:0 4px;">❌ Rejected</button>
    </div>
  `;
  container.insertBefore(filterBar, document.querySelector(".link-section"));

  document.getElementById("filterAccept").addEventListener("click", () => loadAthletesByStatus("accept"));
  document.getElementById("filterPending").addEventListener("click", () => loadAthletesByStatus("pending"));
  document.getElementById("filterRefuse").addEventListener("click", () => loadAthletesByStatus("refuse"));
});

async function loadCoach(cid) {
  try {
    const res = await fetch(`${API}/coaches/${cid}`, { headers: getAuthHeaders() });
    if (!res.ok) throw new Error("Failed to load coach");
    const coach = await res.json();
    document.getElementById("coachName").textContent = `Athletes Directory`;
  } catch (err) {
    console.error("Error loading coach:", err);
  }
}

function normalizeStatus(s) {
  if (!s) return s;
  const v = String(s).toLowerCase();
  return v === "reject" ? "refuse" : v;
}

async function loadAthletesByStatus(status) {
  const cid = await getCurrentCoachId();
  if (!cid) return;

  const backendStatus = normalizeStatus(status);

  try {
    const res = await fetch(`${API}/coaches/${cid}/athletes/${backendStatus}`, { headers: getAuthHeaders() });
    if (!res.ok) throw new Error(`Failed to load athletes: ${res.status}`);
    const raw = await res.json();

    athletes = raw.map(a => {
      return {
        id: a.athlete_id,
        name: a.name || "Unknown",
        status: normalizeStatus(a.relation_status || backendStatus)
      };
    });

    renderDataTable(backendStatus);
  } catch (err) {
    console.error("Error loading athletes:", err);
    showToast("⚠️ Failed to load athletes", "error");
  }
}

function renderDataTable(status) {
  let columns = [];

  if (status === "accept") {
    columns = [
      { data: "id", title: "ID" },
      { data: "name", title: "Name" },

      {
        data: null,
        title: "Divisions",
        render: (a) => `
        <div class="division-btns">
          <button class="runner" onclick="openDivision(${a.id}, '${a.name}', 'runner')">Runner</button>
          <button class="jumper" onclick="openDivision(${a.id}, '${a.name}', 'jumper')">Jumper</button>
          <button class="throw" onclick="openDivision(${a.id}, '${a.name}', 'throw')">Throw</button>
          <button class="gym" onclick="openDivision(${a.id}, '${a.name}', 'gym')">Gym</button>
          <button class="marathon" onclick="openDivision(${a.id}, '${a.name}', 'marathon')">Marathon</button>
        </div>`
      },

      {
        data: null,
        title: "Plan",
        render: (a) =>
            `<button class="plan-btn" onclick="openPlan(${a.id})">Open Plan</button>`
      },

      {
        data: null,
        title: "Remove",
        render: (a) =>
            `<button class="unlink-btn" onclick="removeLink(${a.id})">
          X
        </button>`
      }
    ];
  }


  else if (status === "pending") {
    columns = [
      { data: "name", title: "Name" },
      {
        data: null,
        title: "Action",
        render: (a) =>
          `<button class="unlink-btn" onclick="updateStatus(${a.id}, 'refuse')">Reject</button>`
      }
    ];
  }

  else if (status === "refuse") {
    columns = [
      { data: "name", title: "Name" },
      {
        data: null,
        title: "Resend",
        render: (a) => {
          const remaining = getRemainingResends(a.id);
          return `
            <button class="link-btn"
              ${remaining <= 0 ? "disabled" : ""}
              onclick="resendRequest(${a.id}, this)">
              Resend (${remaining} left)
            </button>`;
        }
      }
    ];
  }

  if ($.fn.DataTable.isDataTable("#athleteTable")) {
    $("#athleteTable").DataTable().clear().destroy();
    $("#athleteTable").empty();
  }

  $("#athleteTable").DataTable({
    data: athletes,
    columns,
    ordering: true,
    searching: true,

    rowCallback: function (row, data) {
      row.classList.remove("row-accept", "row-pending", "row-refuse");

      if (data.status === "accept")  row.classList.add("row-accept");
      if (data.status === "pending") row.classList.add("row-pending");
      if (data.status === "refuse")  row.classList.add("row-refuse");
    },

    language: { emptyTable: `No ${status} athletes.` }
  });
}
async function updateStatus(athleteId, newStatusRaw) {
  const cid = await getCurrentCoachId();
  if (!cid) return;

  const newStatus = normalizeStatus(newStatusRaw);

  try {
    const res = await fetch(`${API}/coaches/${cid}/athletes/${athleteId}/status/${newStatus}`, {
      method: "PUT",
      headers: getAuthHeaders()
    });

    if (res.ok) {
      showToast(`✅ Updated to "${newStatus}"`);
      loadAthletesByStatus(newStatus);
    } else {
      const t = await res.text();
      showToast(`⚠️ Failed: ${t}`, "error");
    }
  } catch (err) {
    console.error(err);
    showToast("❌ Network error", "error");
  }
}

function getRemainingResends(athleteId) {
  const cid = localStorage.getItem("coachId") || coachIdFromUrl || "unknown";
  const key = `resendCount_${cid}_${athleteId}`;
  const today = new Date().toDateString();
  const stored = JSON.parse(localStorage.getItem(key)) || { date: null, count: 0 };

  if (stored.date !== today) {
    localStorage.setItem(key, JSON.stringify({ date: today, count: 0 }));
    return 3;
  }

  return Math.max(0, 3 - stored.count);
}

function incrementResend(athleteId) {
  const cid = localStorage.getItem("coachId") || coachIdFromUrl || "unknown";
  const key = `resendCount_${cid}_${athleteId}`;
  const today = new Date().toDateString();
  const stored = JSON.parse(localStorage.getItem(key)) || { date: today, count: 0 };
  const newCount = stored.date === today ? stored.count + 1 : 1;
  localStorage.setItem(key, JSON.stringify({ date: today, count: newCount }));
}

async function resendRequest(athleteId, btnEl) {
  const remaining = getRemainingResends(athleteId);
  if (remaining <= 0) {
    showToast("⚠️ Daily limit reached (3)", "error");
    return;
  }

  const cid = await getCurrentCoachId();
  if (!cid) return;

  try {
    const res = await fetch(`${API}/coaches/${cid}/athletes/${athleteId}/status/pending`, {
      method: "PUT",
      headers: getAuthHeaders()
    });

    if (!res.ok) {
      const text = await res.text();
      showToast(`❌ Failed: ${text}`, "error");
      return;
    }

    incrementResend(athleteId);

    const left = getRemainingResends(athleteId);
    if (btnEl) {
      btnEl.textContent = `🔁 Resend (${left} left)`;
      if (left <= 0) btnEl.disabled = true;
    }

    showToast("✅ Request resent");
    loadAthletesByStatus("pending");

  } catch (err) {
    console.error(err);
    showToast("❌ Network error", "error");
  }
}

async function linkAthlete() {
  const athleteId = document.getElementById("linkAthleteId").value.trim();
  if (!athleteId) {
    showToast("⚠️ Enter an athlete ID!", "error");
    return;
  }

  const cid = await getCurrentCoachId();
  if (!cid) return;

  try {
    const res = await fetch(`${API}/coaches/${cid}/athletes/${athleteId}`, {
      method: "POST",
      headers: getAuthHeaders()
    });

    if (!res.ok) {
      const msg = await res.text();
      showToast(`❌ ${msg}`, "error");
      return;
    }

    showToast("✅ Request sent");
    document.getElementById("linkAthleteId").value = "";
    loadAthletesByStatus("pending");

  } catch (e) {
    console.error(e);
    showToast("❌ Failed", "error");
  }
}

async function sendUsernameLink() {
  const name = document.getElementById("athUsername").value.trim();
  if (!name) {
    showToast("⚠️ Enter athlete NAME!", "error");
    return;
  }

  const cid = await getCurrentCoachId();
  if (!cid) return;

  let athList = [];
  try {
    const res = await fetch(`${API}/athletes`, { headers: getAuthHeaders() });
    if (!res.ok) throw new Error();
    athList = await res.json();
  } catch (err) {
    showToast("❌ Could not load athletes", "error");
    return;
  }

  const found = athList.find(a =>
    a.name?.toLowerCase() === name.toLowerCase()
  );

  if (!found) {
    showToast("❌ No athlete found with that NAME", "error");
    return;
  }

  const athleteId = found.id;

  try {
    const res = await fetch(`${API}/coaches/${cid}/athletes/${athleteId}`, {
      method: "POST",
      headers: getAuthHeaders()
    });

    if (!res.ok) {
      const msg = await res.text();
      showToast(`❌ ${msg}`, "error");
      return;
    }

    showToast(`✅ Request sent to ${found.name}`);
    document.getElementById("athUsername").value = "";
    loadAthletesByStatus("pending");

  } catch (err) {
    console.error(err);
    showToast("❌ Failed", "error");
  }
}

function openDivision(id, name, type) {
  localStorage.setItem("selectedAthlete", JSON.stringify({ id, name }));
  const map = {
    runner: "./work/new-runner.html",
    jumper: "./work/jumper.html",
    throw: "./work/throw.html",
    gym: "./work/gym.html",
    marathon: "./work/marathon.html"
  };
  window.location.href = `${map[type]}?id=${id}`;
}

function openPlan(athleteId) {
  const cid = localStorage.getItem("coachId") || coachIdFromUrl;
  window.location.href = `plan.html?athleteId=${athleteId}&coachId=${cid}`;
}

function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => (toast.style.opacity = "1"), 100);
  setTimeout(() => {
    toast.style.opacity = "0";
    setTimeout(() => toast.remove(), 400);
  }, 2500);
}

let allAthletesCached = [];

async function loadAllAthletesForSearch() {
  try {
    const res = await fetch(`${API}/athletes`, { headers: getAuthHeaders() });
    if (!res.ok) return;

    allAthletesCached = await res.json();
  } catch (err) {
    console.error("Failed to load athletes for autocomplete");
  }
}

loadAllAthletesForSearch();

const input = document.getElementById("athUsername");
const suggestionBox = document.getElementById("nameSuggestions");

input.addEventListener("input", () => {
  const text = input.value.toLowerCase();
  if (!text) {
    suggestionBox.style.display = "none";
    return;
  }

  const matches = allAthletesCached.filter(a =>
    a.name?.toLowerCase().includes(text)
  );

  if (matches.length === 0) {
    suggestionBox.style.display = "none";
    return;
  }

  suggestionBox.innerHTML = matches
    .map(m => `<div class="suggestion-item"
                  style="padding:8px; cursor:pointer;"
                  onclick="selectAthleteSuggestion('${m.name}')">
                  ${m.name}
               </div>`)
    .join("");

  suggestionBox.style.display = "block";
});

function selectAthleteSuggestion(name) {
  input.value = name;
  suggestionBox.style.display = "none";
}

document.addEventListener("click", (e) => {
  if (e.target !== input) {
    suggestionBox.style.display = "none";
  }
});
async function removeLink(athleteId) {
  const cid = await getCurrentCoachId();
  if (!cid) return;

  if (!confirm("❗ Are you sure you want to remove this athlete?")) return;

  try {
    const res = await fetch(`${API}/coaches/${cid}/athletes/${athleteId}`, {
      method: "DELETE",
      headers: getAuthHeaders()
    });

    if (res.ok) {
      showToast("❌ Athlete unlinked successfully");
      loadAthletesByStatus("accept");
    } else {
      const msg = await res.text();
      showToast(`⚠️ Failed: ${msg}`, "error");
    }
  } catch (err) {
    console.error(err);
    showToast("❌ Network error", "error");
  }
}
