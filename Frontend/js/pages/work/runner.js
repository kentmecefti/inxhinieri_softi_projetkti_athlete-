
const API = window.env?.API || "http://localhost:8080/api";
const raceTypes = ["Start", "Hyrje", "Zbritje", "Ngjitje", "Pengesa"];
const distances = [20,30,40,50,60,70,80,100,110,120,125,150,175,200,225,300,400,500,600,800,1000];

let athlete = null;
let chart = null;
let allResults = [];
let showingAll = false;

function getAuthHeaders() {
  const token = localStorage.getItem("token");
  if (!token) {
    showMessage("Session expired. Please log in again.", "error");
    setTimeout(() => (window.location.href = "login.html"), 1000);
    return {};
  }
  return {
    Authorization: `Bearer ${token}`,
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

const params = new URLSearchParams(window.location.search);
const athleteId = params.get("id");

if (!athleteId) {
  showMessage("❌ No athlete ID found!", "error");
  setTimeout(() => (window.location.href = "athletes.html"), 1000);
}

document.addEventListener("DOMContentLoaded", () => {
  loadAthlete();
  populateDropdowns();
  loadResults();
});

async function loadAthlete() {
  try {
    athlete = await apiFetch(`${API}/athletes/${athleteId}`);
    document.getElementById("athleteNameTitle").textContent = athlete.name;
  } catch {}
}

async function loadResults() {
  try {
    showingAll = false;
    const results = await apiFetch(`${API}/results/athlete/${athleteId}`);
    renderResults(results);
  } catch {}
}

async function addResult() {
  const race = document.getElementById("race").value;
  const date = document.getElementById("date").value;
  const raceType = document.getElementById("raceTypeSelect").value;
  const distance = Number(document.getElementById("distanceSelectInput").value);
  const seconds = Number(document.getElementById("seconds").value);
  const weightValue = document.getElementById("weight").value;
  const notes = document.getElementById("notes").value.trim();
  const weight = weightValue ? Number(weightValue) : null;

  if (!race || !date || !raceType || !distance || seconds <= 0)
    return showMessage("⚠️ Please fill all fields correctly.", "error");

  const payload = {
    athleteId,
    race,
    raceType,
    raceDate: date,
    distance,
    timeMs: Math.round(seconds * 1000),
    weight,
    notes
  };

  try {
    await apiFetch(`${API}/results`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
    showMessage("✅ Result added!", "success");
    document.getElementById("seconds").value = "";
    document.getElementById("notes").value = "";
    loadResults();
  } catch {}
}

async function deleteResult(id) {
  if (!confirm("Delete this result?")) return;
  try {
    await apiFetch(`${API}/results/${id}`, { method: "DELETE" });
    loadResults();
  } catch {}
}

function renderResults(results, forceAll = false) {
  const list = document.getElementById("resultsList");
  const showMoreBtn = document.getElementById("showMoreBtn");

  list.classList.add("show"); 
  allResults = results || [];
  list.innerHTML = "";

  if (!results || results.length === 0) {
    list.innerHTML = '<li class="empty">No results yet for this athlete.</li>';
    showMoreBtn.style.display = "none";
    return;
  }

  const visible = (showingAll || forceAll) ? results : results.slice(0, 5);

  visible.forEach(r => {
    const li = document.createElement("li");
    li.innerHTML = `
      <span>${r.raceDate} • ${r.raceType} (${r.distance}m) • ${(r.timeMs/1000).toFixed(3)}s</span>
      <button class="delete-btn" onclick="deleteResult(${r.id || r.resultId})">🗑️</button>
    `;
    list.appendChild(li);
  });

  showMoreBtn.style.display =
    (!forceAll && results.length > 5) ? "block" : "none";
}


function toggleResults() {
  showingAll = !showingAll;
  renderResults(allResults);
}

document.getElementById("showMoreBtn").addEventListener("click", toggleResults);

async function updateChart() {
  const race = document.getElementById("chartRace").value;
  const raceType = document.getElementById("chartRaceType").value;
  const distance = document.getElementById("chartDistance").value;
  const weightValue = document.getElementById("chartWeight").value;
  const from = document.getElementById("startDate").value;
  const to = document.getElementById("endDate").value;
  const chartWrapper = document.querySelector(".chart-wrapper");

  if (!race || !raceType || !distance) return;

  const p = new URLSearchParams({ athleteId, distance });
  p.append("race", race);
  p.append("raceType", raceType);
  if (weightValue && weightValue !== "none") p.append("weight", weightValue);
  if (from) p.append("from", from);
  if (to) p.append("to", to);

  try {
    const results = await apiFetch(`${API}/results/filter?${p.toString()}`);

    if (!results || !results.length) {
      if (chart) chart.destroy();
      chartWrapper.classList.remove("show");
      return;
    }

    chartWrapper.classList.add("show");
    results.sort((a, b) => new Date(a.raceDate) - new Date(b.raceDate));

    const labels = results.map(r => r.raceDate);
    const data = results.map(r => r.timeMs / 1000);

    if (chart) chart.destroy();

    chart = new Chart(document.getElementById("raceChart"), {
      type: "line",
      data: {
        labels,
        datasets: [{
          label: `${athlete.name} — ${raceType} (${distance}m)`,
          data,
          borderColor: "#2563eb",
          backgroundColor: "rgba(37,99,235,0.2)",
          tension: 0.25,
          pointRadius: 5
        }]
      },
      options: { responsive: true }
    });
  } catch {}
}

function populateDropdowns() {
  ["raceTypeSelect","chartRaceType","searchRaceType"].forEach(id => {
    const sel = document.getElementById(id);
    sel.innerHTML = "";
    raceTypes.forEach(r => sel.add(new Option(r, r)));
  });

  ["distanceSelectInput","chartDistance","searchDistance"].forEach(id => {
    const sel = document.getElementById(id);
    sel.innerHTML = "";
    distances.forEach(d => sel.add(new Option(`${d}m`, d)));
  });
}

async function applySearchFilters() {
  showingAll = true;

  const url = new URL(`${API}/results/search`);
  url.searchParams.append("athleteId", athleteId);

  const map = {
    race: "searchRace",
    raceType: "searchRaceType",
    distance: "searchDistance",
    weight: "searchWeight",
    fromDate: "searchFrom",
    toDate: "searchTo"
  };

  for (const [k, id] of Object.entries(map)) {
    const v = document.getElementById(id).value;
    if (v) url.searchParams.append(k, v);
  }

  url.searchParams.append("sortBy", document.getElementById("searchSortBy").value || "date");
  url.searchParams.append("sortOrder", document.getElementById("searchSortOrder").value || "asc");

  try {
    const results = await apiFetch(url.toString());
    renderResults(results, true);
  } catch {}
}

function clearSearchFilters() {
  ["searchRace","searchRaceType","searchDistance","searchWeight","searchFrom","searchTo"]
    .forEach(id => document.getElementById(id).value = "");

  loadResults();
  showMessage("🔄 Filters cleared", "success");
}

function showMessage(text, type="success") {
  const msg = document.createElement("div");
  msg.className = `alert ${type}`;
  msg.textContent = text;
  document.body.appendChild(msg);
  setTimeout(() => msg.classList.add("show"), 50);
  setTimeout(() => msg.remove(), 2500);
}

["chartRace","chartRaceType","chartDistance","chartWeight","startDate","endDate"]
  .forEach(id => document.getElementById(id)?.addEventListener("change", updateChart));

document.querySelectorAll(".tab-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
    btn.classList.add("active");
    document.getElementById(btn.dataset.tab).classList.add("active");
  });
});
