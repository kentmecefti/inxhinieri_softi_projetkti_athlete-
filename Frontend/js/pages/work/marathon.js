
const BASE_API = window.env?.API || "http://localhost:8080/api";
const params = new URLSearchParams(window.location.search);
const athleteId = params.get("id");
let allSessions = [];

function getAuthHeaders() {
  const token = localStorage.getItem("token");
  if (!token) {
    showMessage("Session expired. Please log in again.", "error");
    setTimeout(() => (window.location.href = "login.html"), 1000);
    return {};
  }
  return {
    "Authorization": `Bearer ${token}`,
    "Content-Type": "application/json"
  };
}

async function apiFetch(url, options = {}) {
  try {
    const res = await fetch(url, { ...options, headers: getAuthHeaders() });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.status !== 204 ? await res.json() : null;
  } catch (err) {
    showMessage(`❌ ${err.message}`, "error");
    throw err;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  if (!athleteId) {
    showMessage("❌ No athlete ID provided!", "error");
    setTimeout(goBack, 1200);
    return;
  }

  document.getElementById("todayDate").textContent = new Date().toLocaleDateString('en-GB', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
  });

  loadAthlete();
  loadSessions();

  document.getElementById("runFilterDate").addEventListener("change", applyDateFilter);
});

function toggleForm() {
  document.getElementById("formModal").classList.toggle("hidden");
}

async function loadAthlete() {
  try {
    const athlete = await apiFetch(`${BASE_API}/athletes/${athleteId}`);
    const nameEl = document.getElementById("athleteName");
    nameEl.textContent = `👤 ${athlete.name}`;
    nameEl.addEventListener("click", () => {
      window.location.href = `athlete.html?id=${athleteId}`;
    });
  } catch (err) {
    const nameEl = document.getElementById("athleteName");
    nameEl.textContent = "Unknown Athlete";
    nameEl.addEventListener("click", goBack);
  }
}

async function loadSessions() {
  try {
    allSessions = await apiFetch(`${BASE_API}/sessions/athlete/${athleteId}`);
    renderSessions(allSessions);
    updateStats(allSessions);
  } catch {
    showMessage("❌ Failed to load sessions", "error");
  }
}

function applyDateFilter(e) {
  const selectedDate = e.target.value;
  if (!selectedDate) {
    renderSessions(allSessions);
    updateStats(allSessions);
    return;
  }
  const filtered = allSessions.filter(s => s.runDate === selectedDate);
  renderSessions(filtered);
  updateStats(filtered);
}

function clearFilter() {
  document.getElementById("runFilterDate").value = "";
  renderSessions(allSessions);
  updateStats(allSessions);
}

function calculateCalories(weightKg, distanceKm) {
  if (!weightKg || !distanceKm) return 0;
  return (weightKg * distanceKm * 1.036).toFixed(2);
}

function updateLiveCalories() {
  const weight = 70;
  const d = parseFloat(document.getElementById("marathonDistance").value);
  document.getElementById("marathonCalories").value = calculateCalories(weight, d);
}

function renderSessions(list) {
  const container = document.getElementById("sessionsList");
  if (!list.length) {
    container.innerHTML = `<p class="muted">No marathon sessions yet.</p>`;
    return;
  }

  container.innerHTML = list.map(s => `
    <div class="session-card">
      <h3>${new Date(s.runDate).toLocaleDateString()}</h3>
      <p>🏃 ${s.distanceKm ?? 0} km | 🕒 ${s.timeMin ?? '-'} min</p>
      <p>❤️ Avg ${s.heartAvg ?? '-'} bpm | Max ${s.heartMax ?? '-'}</p>
      <p>🔥 ${s.calories ?? 0} kcal | 🌦️ ${s.weather ?? ''}</p>
      <p>🛣️ ${s.surface ?? ''}</p>
      <small>${s.notes ?? ''}</small>
      <div class="actions">
        <button onclick="deleteSession(${s.sessionId})" class="danger">🗑 Delete</button>
      </div>
    </div>
  `).join("");
}

function updateStats(list) {
  const totalRuns = list.length;
  if (!totalRuns) {
    document.getElementById("statRuns").textContent = 0;
    document.getElementById("statTime").textContent = 0;
    document.getElementById("statDist").textContent = 0;
    document.getElementById("statHeart").textContent = 0;
    return;
  }

  const totalTime = list.reduce((sum, s) => sum + Number(s.timeMin || 0), 0);
  const totalDist = list.reduce((sum, s) => sum + Number(s.distanceKm || 0), 0);
  const avgHR = Math.round(list.reduce((sum, s) => sum + Number(s.heartAvg || 0), 0) / totalRuns);

  document.getElementById("statRuns").textContent = totalRuns;
  document.getElementById("statTime").textContent = totalTime.toFixed(1);
  document.getElementById("statDist").textContent = totalDist.toFixed(2);
  document.getElementById("statHeart").textContent = isNaN(avgHR) ? 0 : avgHR;
}

async function addMarathon() {
  const data = {
    athleteId: parseInt(athleteId),
    runDate: document.getElementById("marathonDate").value,
    timeMin: parseFloat(document.getElementById("marathonTimeMin").value),
    distanceKm: parseFloat(document.getElementById("marathonDistance").value),
    heartAvg: parseInt(document.getElementById("marathonHeartAvg").value) || null,
    heartMax: parseInt(document.getElementById("marathonHeartMax").value) || null,
    calories: parseFloat(document.getElementById("marathonCalories").value) || 0,
    surface: document.getElementById("marathonSurface").value,
    weather: document.getElementById("marathonWeather").value,
    notes: document.getElementById("marathonNotes").value
  };

  try {
    await apiFetch(`${BASE_API}/sessions`, {
      method: "POST",
      body: JSON.stringify(data)
    });
    toggleForm();
    await loadSessions();
    document.getElementById("sessionForm").reset();
    showMessage("✅ Marathon session added!");
  } catch {
    showMessage("❌ Failed to add session", "error");
  }
}

async function deleteSession(id) {
  if (!confirm("Delete this session?")) return;
  try {
    await apiFetch(`${BASE_API}/sessions/${id}`, { method: "DELETE" });
    await loadSessions();
    showMessage("🗑 Session deleted");
  } catch {
    showMessage("❌ Failed to delete session", "error");
  }
}

function showMessage(text, type = "success") {
  const msg = document.createElement("div");
  msg.className = `alert ${type}`;
  msg.textContent = text;
  document.body.appendChild(msg);
  setTimeout(() => msg.classList.add("show"), 50);
  setTimeout(() => {
    msg.classList.remove("show");
    setTimeout(() => msg.remove(), 500);
  }, 2500);
}
