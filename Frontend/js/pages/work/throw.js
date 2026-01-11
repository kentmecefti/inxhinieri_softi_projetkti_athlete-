
const BASE_API = window.env?.API || "http://localhost:8080/api";
const API = `${BASE_API}/throwresults`;

let athleteId = null;
let throwsAll = [];
let filtered = [];
let chartSets = [];
let currentChartIndex = 0;
let activeChart = null;

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

function getQS(name) {
  return new URLSearchParams(window.location.search).get(name);
}
function toNum(v) {
  const n = Number(v);
  return isNaN(n) ? null : n;
}
function fmtDate(d) {
  return new Date(d).toISOString().slice(0, 10);
}

document.addEventListener("DOMContentLoaded", () => {
  athleteId = getQS("id");

  if (!athleteId || isNaN(athleteId)) {
    showMessage("❌ Invalid athlete ID — redirecting...", "error");
    setTimeout(() => (window.location.href = "athletes.html"), 1200);
    return;
  }

  loadAthleteTitle();
  loadThrows();
});

async function loadAthleteTitle() {
  try {
    const a = await apiFetch(`${BASE_API}/athletes/${athleteId}`);
    document.getElementById("athleteTitle").textContent = a.name || "Athlete";
  } catch (err) {}
}

async function loadThrows() {
  throwsAll = [];
  filtered = [];

  try {
    const res = await apiFetch(`${API}?athleteId=${athleteId}`);
    throwsAll = res || [];
    filtered = throwsAll.filter(t => t.athlete?.id == athleteId);
    render();
  } catch (err) {
    showMessage("❌ Could not load throw results", "error");
  }
}

async function addThrow() {
  const throwType = document.getElementById("sessionType").value;
  const event = document.getElementById("event").value;
  const throwStyle = document.getElementById("throwStyle").value;
  const date = document.getElementById("date").value || fmtDate(new Date());
  const distance = toNum(document.getElementById("distance").value);
  const wind = toNum(document.getElementById("wind").value);
  const notes = document.getElementById("notes").value.trim();

  if (!distance || distance <= 0)
    return showMessage("⚠️ Enter a valid distance", "error");

  const body = {
    athlete: { id: Number(athleteId) },
    throwDate: date,
    throwType,
    event,
    throwStyle,
    distance,
    wind,
    notes,
  };

  try {
    await apiFetch(API, {
      method: "POST",
      body: JSON.stringify(body)
    });
    showMessage("✅ Throw added!");
    await loadThrows();
    resetAddForm();
  } catch (err) {}
}

function resetAddForm() {
  ["distance", "wind", "notes", "throwStyle"].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = "";
  });
}

async function deleteThrow(id) {
  if (!confirm("Delete this throw?")) return;
  try {
    await apiFetch(`${API}/${id}`, { method: "DELETE" });
    showMessage("🗑️ Deleted throw");
    await loadThrows();
  } catch (err) {}
}

async function applySearch() {
  const event = document.getElementById("searchEvent").value;
  const throwType = document.getElementById("searchSession").value;
  const throwStyle = document.getElementById("searchStyle")?.value;
  const from = document.getElementById("searchFrom").value;
  const to = document.getElementById("searchTo").value;
  const min = toNum(document.getElementById("searchMin")?.value);
  const max = toNum(document.getElementById("searchMax")?.value);
  const sortBy = document.getElementById("sortBy").value || "date";
  const sortOrder = document.getElementById("sortOrder").value || "desc";

  const params = new URLSearchParams();
  params.append("athleteId", athleteId);
  if (event) params.append("event", event);
  if (throwType) params.append("throwType", throwType);
  if (throwStyle) params.append("throwStyle", throwStyle);
  if (from) params.append("from", from);
  if (to) params.append("to", to);
  if (min != null) params.append("min", min);
  if (max != null) params.append("max", max);
  params.append("sortBy", sortBy);
  params.append("sortOrder", sortOrder);

  try {
    filtered = await apiFetch(`${API}/search?${params.toString()}`);
    filtered = filtered.filter(t => t.athlete?.id == athleteId);

    filtered.sort((a, b) => {
      let valA, valB;
      if (sortBy === "date") {
        valA = new Date(a.throwDate);
        valB = new Date(b.throwDate);
      } else if (sortBy === "distance") {
        valA = parseFloat(a.distance || 0);
        valB = parseFloat(b.distance || 0);
      }
      return sortOrder === "asc" ? valA - valB : valB - valA;
    });

    render();
    updateChart();
    showMessage(`🔍 Sorted by ${sortBy} (${sortOrder.toUpperCase()})`);
  } catch (err) {
    showMessage("❌ Failed to apply search filters", "error");
  }
}

function render() {
  const count = filtered.length;
  const best = count ? Math.max(...filtered.map(t => t.distance)) : null;
  const avg = count ? filtered.reduce((a, t) => a + t.distance, 0) / count : null;

  document.getElementById("statCount").textContent = count;
  document.getElementById("statBest").textContent = best ? best.toFixed(2) : "—";
  document.getElementById("statAvg").textContent = avg ? avg.toFixed(2) : "—";

  const ul = document.getElementById("throwList");
  ul.innerHTML = count
    ? filtered.map(t => `
        <li>
          <div>
            <div class="title">${t.event} •
              <span class="highlight">${t.distance.toFixed(2)} m</span>
            </div>
            <div class="meta">
              <span>${t.throwType}</span>
              ${t.throwStyle ? ` • Style: ${t.throwStyle}` : ""}
              • <span>${t.throwDate}</span>
              ${t.wind != null ? ` • Wind: ${t.wind} m/s` : ""}
              ${t.notes ? ` • ${t.notes}` : ""}
            </div>
          </div>
          <button class="delete-btn" onclick="deleteThrow(${t.id})">🗑️</button>
        </li>`).join("")
    : "<li class='empty'>No throws found.</li>";
}

async function updateChart() {
  if (!athleteId) return;

  const filtEvent = document.getElementById("chartEvent")?.value || "";
  const from = document.getElementById("chartFrom")?.value || "";
  const to = document.getElementById("chartTo")?.value || "";

  const params = new URLSearchParams({ athleteId });
  if (filtEvent) params.append("event", filtEvent);
  if (from) params.append("from", from);
  if (to) params.append("to", to);

  const container =
    document.getElementById("chartsContainer") ||
    document.querySelector(".chart-wrapper");

  container.classList.remove("show");
  container.innerHTML = "<p class='muted'>⏳ Loading chart...</p>";

  try {
    const throws = await apiFetch(`${API}/filter?${params.toString()}`);
    container.innerHTML = "";

    if (!throws.length) {
      container.innerHTML = "<p class='empty'>No throw data found.</p>";
      return;
    }

    const grouped = {};
    throws.forEach(t => {
      const key = t.event || "(unknown event)";
      if (!grouped[key]) grouped[key] = [];
      grouped[key].push(t);
    });

    chartSets = Object.entries(grouped).map(([label, list]) => ({
      label,
      data: list.sort((a, b) => new Date(a.throwDate) - new Date(b.throwDate))
    }));

    currentChartIndex = 0;
    showChart();

    setTimeout(() => container.classList.add("show"), 150);
  } catch (err) {
    container.innerHTML = "<p class='empty error'>❌ Failed to load chart data.</p>";
  }
}

function showChart() {
  const container =
    document.getElementById("chartsContainer") ||
    document.querySelector(".chart-wrapper");
  container.innerHTML = "";

  if (!chartSets.length) {
    container.innerHTML = "<p class='empty'>No chart data available.</p>";
    return;
  }

  const { label, data } = chartSets[currentChartIndex];
  const title = document.createElement("h3");
  title.textContent = label;
  title.style.textAlign = "center";
  title.style.marginBottom = "10px";
  container.appendChild(title);

  const canvas = document.createElement("canvas");
  canvas.id = "throwChartView";
  container.appendChild(canvas);

  const ctx = canvas.getContext("2d");
  if (activeChart) activeChart.destroy();

  activeChart = new Chart(ctx, {
    type: "line",
    data: {
      labels: data.map(t => t.throwDate),
      datasets: [{
        label: `${data[0].athlete?.name || "Athlete"} — ${label}`,
        data: data.map(t => t.distance),
        borderColor: "#2563eb",
        backgroundColor: "rgba(37,99,235,0.2)",
        borderWidth: 2,
        tension: 0.25,
        pointRadius: 5,
        pointHoverRadius: 7,
        pointBackgroundColor: "#2563eb"
      }]
    },
    options: {
      responsive: true,
      interaction: { mode: "nearest", intersect: false },
      scales: {
        y: { title: { display: true, text: "Distance (m)" } },
        x: { title: { display: true, text: "Date" } }
      },
      plugins: {
        legend: { display: true },
        datalabels: {
          align: "top",
          anchor: "end",
          color: "#1f2d3d",
          font: { weight: "bold", size: 11 },
          formatter: v => `${v.toFixed(2)} m`
        },
        tooltip: {
          usePointStyle: true,
          callbacks: {
            label: context => {
              const t = data[context.dataIndex];
              const notes = t.notes ? `📝 ${t.notes}` : "";
              return [
                `Date: ${t.throwDate}`,
                `Event: ${t.event}`,
                `Type: ${t.throwType || "—"}`,
                `Style: ${t.throwStyle || "—"}`,
                `Distance: ${t.distance.toFixed(2)} m`,
                `Wind: ${t.wind != null ? t.wind + " m/s" : "—"}`,
                notes
              ];
            },
            title: () => ""
          }
        }
      }
    },
    plugins: [ChartDataLabels]
  });

  const nav = document.createElement("div");
  nav.className = "chart-nav";
  nav.style.textAlign = "center";
  nav.style.marginTop = "10px";
  nav.innerHTML = `
    <button class="ghost small" onclick="prevChart()">← Prev</button>
    <span style="margin:0 8px;">${currentChartIndex + 1} / ${chartSets.length}</span>
    <button class="ghost small" onclick="nextChart()">Next →</button>
  `;
  container.appendChild(nav);
}

function nextChart() {
  if (!chartSets.length) return;
  currentChartIndex = (currentChartIndex + 1) % chartSets.length;
  showChart();
}

function prevChart() {
  if (!chartSets.length) return;
  currentChartIndex = (currentChartIndex - 1 + chartSets.length) % chartSets.length;
  showChart();
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

document.querySelectorAll(".tab-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
    btn.classList.add("active");
    document.getElementById(btn.dataset.tab).classList.add("active");
  });
});
