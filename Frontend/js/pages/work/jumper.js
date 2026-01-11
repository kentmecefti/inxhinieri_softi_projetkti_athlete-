
const API = window.env?.API || "http://localhost:8080/api";
let athleteId = null;
let athlete = null;
let chart = null;
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

const params = new URLSearchParams(window.location.search);
athleteId = params.get("id");
if (!athleteId) {
  showMessage("❌ No athlete ID found!", "error");
  setTimeout(() => (window.location.href = "athletes.html"), 1200);
}

document.addEventListener("DOMContentLoaded", () => {
  loadAthleteData();
  loadJumps();
  populateSearchAndChartSelects();
});

async function loadAthleteData() {
  try {
    athlete = await apiFetch(`${API}/athletes/${athleteId}`);
    document.getElementById("athleteNameTitle").textContent = athlete.name;
  } catch (err) {}
}

async function loadJumps() {
  try {
    const jumps = await apiFetch(`${API}/jumpresults`);

    const filtered = jumps.filter(j => String(j.athleteId) === String(athleteId));

    renderJumps(filtered);
  } catch (err) {}
}


function updateOptions() {
  const type = document.getElementById("jumpType").value;
  const optionsDiv = document.getElementById("jumpOptions");
  optionsDiv.innerHTML = "";

  if (type === "shhapes") {
    optionsDiv.innerHTML = `
      <label>Shhapes Distance:</label>
      <select id="shhapesDistance" onchange="updateDetail()">
        <option value="50m">50m</option>
        <option value="80m">80m</option>
        <option value="100m">100m</option>
      </select>
      <label>Style:</label>
      <select id="shhapesStyle" onchange="updateDetail()">
        <option value="Shpejt">Shpejt</option>
        <option value="Te Madh">Te Madh</option>
        <option value="Both">Both</option>
      </select>`;
  } else if (type === "ngaVendi") {
    optionsDiv.innerHTML = `
      <label>Steps:</label>
      <select id="vendiSteps" onchange="updateDetail()">
        <option value="1 hap">1 hap</option>
        <option value="2hapesh">2hapesh</option>
        <option value="3hapesh">3hapesh</option>
        <option value="5hapesh">5hapesh</option>
        <option value="10hapesh">10hapesh</option>
      </select>`;
  }

  updateDetail();
}

function updateDetail() {
  const type = document.getElementById("jumpType").value;
  let detail = "";

  if (type === "shhapes") {
    const dist = document.getElementById("shhapesDistance")?.value || "";
    const style = document.getElementById("shhapesStyle")?.value || "";
    detail = `${dist} - ${style}`;
  } else if (type === "ngaVendi") {
    detail = document.getElementById("vendiSteps")?.value || "";
  }

  document.getElementById("detail").value = detail;
}

async function addJump() {
  const jumpType = document.getElementById("jumpType").value;
  const date = document.getElementById("jumpDate").value;
  const detail = document.getElementById("detail").value.trim();
  const distanceM = parseFloat(document.getElementById("distanceM").value);
  const notes = document.getElementById("notes").value.trim();

  if (!jumpType || !date || isNaN(distanceM) || distanceM <= 0)
    return showMessage("⚠️ Please fill all fields correctly", "error");

  const payload = { athleteId, jumpDate: date, jumpType, detail, distanceM, notes };

  try {
    await apiFetch(`${API}/jumpresults`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
    showMessage("✅ Jump added!");
    document.getElementById("distanceM").value = "";
    document.getElementById("notes").value = "";
    loadJumps();
  } catch (err) {}
}

async function deleteJump(id) {
  if (!confirm("Delete this jump?")) return;
  try {
    await apiFetch(`${API}/jumpresults/${id}`, { method: "DELETE" });
    loadJumps();
  } catch (err) {}
}

function renderJumps(jumps) {
  const list = document.getElementById("jumpResultsList");
  list.innerHTML = "";

  if (!jumps.length) {
    list.innerHTML = "<li>No jumps yet.</li>";
    return;
  }

  jumps.forEach(j => {
    const li = document.createElement("li");
    li.className = "jump-item";
    li.innerHTML = `
      <div class="jump-main">
        ${j.jumpDate} • ${j.jumpType} • ${j.distanceM.toFixed(2)} m
        ${j.detail ? `(${j.detail})` : ""}
      </div>
      ${j.notes ? `<div class="jump-notes">📝 ${j.notes}</div>` : ""}
    `;
    const del = document.createElement("button");
    del.textContent = "🗑️";
    del.className = "delete-btn";
    del.onclick = () => deleteJump(j.jumpId);
    li.append(del);
    list.appendChild(li);
  });
}

function populateSearchAndChartSelects() {
  const jumpTypes = ["", "shhapes", "ngaVendi", "meVrull", "tripleJump", "highJump"];
  const selects = ["searchJumpType", "chartJumpType"];

  selects.forEach(id => {
    const sel = document.getElementById(id);
    sel.innerHTML = jumpTypes.map(j => `<option value="${j}">${j || "All"}</option>`).join("");
  });

  document.getElementById("searchSortBy").value = "jumpDate";
  document.getElementById("searchSortOrder").value = "desc";
}

async function applySearchFilters() {
  const jumpType = document.getElementById("searchJumpType").value;
  const from = document.getElementById("searchFrom").value;
  const to = document.getElementById("searchTo").value;
  const sortBy = document.getElementById("searchSortBy").value;
  const sortOrder = document.getElementById("searchSortOrder").value;

  const params = new URLSearchParams({ athleteId, sortBy, sortOrder });
  if (jumpType) params.append("jumpType", jumpType);
  if (from) params.append("from", from);
  if (to) params.append("to", to);

  try {
    const jumps = await apiFetch(`${API}/jumpresults/search?${params.toString()}`);
    const list = document.getElementById("jumpResultsList");

    if (!jumps.length) {
      list.innerHTML = "<li class='empty'>No jumps found.</li>";
      list.classList.add("show");
      return;
    }

    renderJumps(jumps);
    list.classList.add("show");
    showMessage("🔍 Filter applied");
  } catch (err) {}
}

function clearSearchFilters() {
  ["searchJumpType","searchFrom","searchTo"].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = "";
  });

  document.getElementById("searchSortBy").value = "jumpDate";
  document.getElementById("searchSortOrder").value = "desc";

  const list = document.getElementById("jumpResultsList");
  list.classList.remove("show");
  list.innerHTML = "";
  loadJumps();
  showMessage("🔄 Filters cleared");
}

async function updateChart() {
  if (!athlete) return;

  const jumpType = document.getElementById("chartJumpType").value;
  const detailFilter = document.getElementById("chartDetail")?.value || "";
  const from = document.getElementById("chartFrom").value;
  const to = document.getElementById("chartTo").value;

  const params = new URLSearchParams({ athleteId: athlete.id });
  if (jumpType) params.append("jumpType", jumpType);
  if (detailFilter) params.append("detail", detailFilter);
  if (from) params.append("from", from);
  if (to) params.append("to", to);

  try {
    const jumps = await apiFetch(`${API}/jumpresults/filter?${params.toString()}`);
    const container = document.getElementById("chartsContainer");
    container.innerHTML = "";

    if (!jumps.length) {
      container.innerHTML = "<p class='empty'>No jumps found.</p>";
      return;
    }

    const grouped = {};
    jumps.forEach(j => {
      const d = j.detail?.trim() || "(no detail)";
      if (!grouped[d]) grouped[d] = [];
      grouped[d].push(j);
    });

    chartSets = Object.entries(grouped).map(([detail, list]) => ({
      detail,
      data: list.sort((a, b) => new Date(a.jumpDate) - new Date(b.jumpDate))
    }));

    currentChartIndex = 0;
    showChart();
  } catch (err) {}
}

function showChart() {
  const container = document.getElementById("chartsContainer");
  container.innerHTML = "";

  if (!chartSets.length) {
    container.innerHTML = "<p class='empty'>No chart data available.</p>";
    return;
  }

  const { detail, data } = chartSets[currentChartIndex];

  const title = document.createElement("h3");
  title.textContent = `Detail: ${detail}`;
  title.style.textAlign = "center";
  title.style.marginBottom = "10px";
  container.appendChild(title);

  const canvas = document.createElement("canvas");
  canvas.id = "jumpChartView";
  container.appendChild(canvas);

  const ctx = canvas.getContext("2d");
  if (activeChart) activeChart.destroy();

  activeChart = new Chart(ctx, {
    type: "line",
    data: {
      labels: data.map(j => j.jumpDate),
      datasets: [{
        label: `${athlete.name} — ${data[0].jumpType || "All"} (${detail})`,
        data: data.map(j => j.distanceM),
        borderColor: "#3cb371",
        backgroundColor: "rgba(60,179,113,0.2)",
        borderWidth: 2,
        tension: 0.25,
        pointRadius: 5,
        pointHoverRadius: 7,
        pointBackgroundColor: "#2ecc71"
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
        tooltip: {
          usePointStyle: true,
          callbacks: {
            label: (context) => {
              const j = data[context.dataIndex];
              const notes = j.notes ? `📝 ${j.notes}` : "";
              return [
                `Date: ${j.jumpDate}`,
                `Type: ${j.jumpType || "—"}`,
                `Distance: ${j.distanceM.toFixed(2)} m`,
                `Weight: ${j.weight || "—"} kg`,
                `Height: ${j.heightM ? j.heightM.toFixed(2) + " m" : "—"}`,
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
  nav.style.textAlign = "center";
  nav.style.marginTop = "10px";

  const prevBtn = document.createElement("button");
  prevBtn.textContent = "◀ Previous";
  prevBtn.className = "ghost";
  prevBtn.disabled = currentChartIndex === 0;
  prevBtn.onclick = () => {
    if (currentChartIndex > 0) {
      currentChartIndex--;
      showChart();
    }
  };

  const nextBtn = document.createElement("button");
  nextBtn.textContent = "Next ▶";
  nextBtn.className = "ghost";
  nextBtn.disabled = currentChartIndex === chartSets.length - 1;
  nextBtn.onclick = () => {
    if (currentChartIndex < chartSets.length - 1) {
      currentChartIndex++;
      showChart();
    }
  };

  const pageInfo = document.createElement("span");
  pageInfo.textContent = ` ${currentChartIndex + 1} / ${chartSets.length} `;
  pageInfo.style.margin = "0 12px";
  pageInfo.style.fontWeight = "600";

  nav.append(prevBtn, pageInfo, nextBtn);
  container.appendChild(nav);
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
