(function () {
  const $ = (s) => document.querySelector(s);
  const BASE_API = window.env?.API || "http://localhost:8080/api";

  function getAuthHeaders() {
    const token = localStorage.getItem("token");
    if (!token) {
      showToast("Session expired. Please log in again.", "error");
      setTimeout(() => (window.location.href = "../../../login.html"), 1000);
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
      showToast(`❌ ${err.message}`, "error");
      throw err;
    }
  }

  function getQS(name) {
    return new URLSearchParams(window.location.search).get(name);
  }

  const athleteId = getQS("id");
  if (!athleteId || isNaN(athleteId)) {
    showToast("❌ No athlete ID found! Redirecting...", "error");
    setTimeout(() => (location.href = "../athletes.html"), 1200);
    return;
  }

  async function loadAthleteName() {
    try {
      const athlete = await apiFetch(`${BASE_API}/athletes/${athleteId}`);
      $(".title").textContent = `Gym Tracker ${athlete.name}`;
    } catch {
      $(".title").textContent = "🏋️ Gym Tracker — Unknown Athlete";
    }
  }

  const todayStr = new Date().toISOString().slice(0, 10);
  $("#date").value = todayStr;

  const categoryEl = $("#category");
  const metricRow = $("#metricRow");
  let allSessions = [];

  function metricFieldsFor(category) {
    if (category === "weight") {
      return `
        <div><label>Sets</label><input id="sets" type="number" min="1"></div>
        <div><label>Reps</label><input id="reps" type="number" min="1"></div>
        <div><label>Weight (kg)</label><input id="weight" type="number" min="0" step="0.5"></div>`;
    }
    if (category === "plyo") {
      return `
        <div><label>Contacts</label><input id="contacts" type="number" min="1"></div>
        <div><label>Height (m)</label><input id="height" type="number" min="0" step="0.01"></div>
        <div><label>Intensity (1–10)</label><input id="intensity" type="number" min="1" max="10"></div>`;
    }
    return `
      <div><label>Reaction Time (ms)</label><input id="reaction" type="number"></div>
      <div><label>Trials</label><input id="trials" type="number"></div>
      <div><label>Best Trial (ms)</label><input id="best" type="number"></div>`;
  }

  function rebuildMetricFields() {
    metricRow.innerHTML = metricFieldsFor(categoryEl.value);
  }
  categoryEl.addEventListener("change", rebuildMetricFields);
  rebuildMetricFields();

  function val(id) {
    const el = document.getElementById(id);
    return el ? +el.value || 0 : 0;
  }

  $("#addBtn").addEventListener("click", async () => {
    const date = $("#date").value || todayStr;
    const category = $("#category").value;
    const name = ($("#name").value || "").trim();
    const notes = ($("#notes").value || "").trim();
    if (!name) return showToast("Enter exercise name!", "error");

    const session = {
      athlete: { id: Number(athleteId) },
      sessionDate: date,
      category,
      exerciseName: name,
      notes
    };

    try {
      const saved = await apiFetch(`${BASE_API}/gymsessions`, {
        method: "POST",
        body: JSON.stringify(session)
      });

      if (category === "weight") {
        await apiFetch(`${BASE_API}/metrics/weight`, {
          method: "POST",
          body: JSON.stringify({
            gymSession: { id: saved.id },
            sets: val("sets"),
            reps: val("reps"),
            weightGym: val("weight")
          })
        });
      } else if (category === "plyo") {
        await apiFetch(`${BASE_API}/metrics/plyo`, {
          method: "POST",
          body: JSON.stringify({
            gymSession: { id: saved.id },
            contacts: val("contacts"),
            height: val("height"),
            intensity: val("intensity")
          })
        });
      } else {
        await apiFetch(`${BASE_API}/metrics/reflex`, {
          method: "POST",
          body: JSON.stringify({
            gymSession: { id: saved.id },
            reactionTimeMs: val("reaction"),
            trials: val("trials"),
            bestTrialMs: val("best")
          })
        });
      }

      clearForm();
      await loadSessions();
      showToast("✅ Entry added successfully!");
    } catch {
      showToast("❌ Failed to save entry", "error");
    }
  });

  function clearForm() {
    $("#name").value = "";
    $("#notes").value = "";
    rebuildMetricFields();
  }
  $("#clearFormBtn").addEventListener("click", clearForm);

  const filterEls = {
    category: $("#filterCategory"),
    search: $("#searchBox"),
    from: $("#fromDate"),
    to: $("#toDate")
  };

  function applyFilters() {
    let filtered = [...allSessions];
    const cat = filterEls.category.value;
    const q = filterEls.search.value.toLowerCase();
    const from = filterEls.from.value;
    const to = filterEls.to.value;

    if (cat !== "all") filtered = filtered.filter(s => s.category === cat);
    if (q) filtered = filtered.filter(s => s.exerciseName.toLowerCase().includes(q));
    if (from) filtered = filtered.filter(s => s.sessionDate >= from);
    if (to) filtered = filtered.filter(s => s.sessionDate <= to);

    renderSessions(filtered);
  }

  Object.values(filterEls).forEach(el => el.addEventListener("input", applyFilters));
  $("#resetFiltersBtn").addEventListener("click", () => {
    filterEls.category.value = "all";
    filterEls.search.value = "";
    filterEls.from.value = "";
    filterEls.to.value = "";
    renderSessions(allSessions);
  });

  $("#exportBtn").addEventListener("click", () => {
    const data = JSON.stringify(allSessions, null, 2);
    const blob = new Blob([data], { type: "application/json" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `gym_sessions_${athleteId}.json`;
    a.click();
  });

  $("#importBtn").addEventListener("click", () => $("#importFile").click());
  $("#importFile").addEventListener("change", (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => {
      try {
        const imported = JSON.parse(ev.target.result);
        showToast(`📂 Imported ${imported.length} sessions`);
        renderSessions(imported);
      } catch {
        showToast("❌ Invalid file", "error");
      }
    };
    reader.readAsText(file);
  });

  function renderSessions(sessions) {
    const tbody = $("#tbody");
    if (!sessions || sessions.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="empty">No entries yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = sessions.map(e => `
      <tr>
        <td>${e.sessionDate}</td>
        <td><span class="tag ${e.category}">${e.category}</span></td>
        <td>${escapeHtml(e.exerciseName)}</td>
        <td>${(e.metrics && e.metrics.length)
          ? e.metrics.map(m => Object.entries(m).map(([k,v])=>`${k}:${v}`).join(', ')).join('<br>')
          : "—"}</td>
        <td>${escapeHtml(e.notes || "")}</td>
        <td><button class="ghost del" data-id="${e.id}">✕</button></td>
      </tr>`
    ).join("");

    tbody.querySelectorAll(".del").forEach(btn =>
      btn.addEventListener("click", async () => {
        const id = btn.dataset.id;
        if (!confirm("Delete this session?")) return;
        try {
          await apiFetch(`${BASE_API}/gymsessions/${id}`, { method: "DELETE" });
          await loadSessions();
          showToast("🗑️ Session deleted");
        } catch {
          showToast("❌ Failed to delete session", "error");
        }
      })
    );
  }

  async function loadSessions() {
    try {
      allSessions = await apiFetch(`${BASE_API}/gymsessions/athlete/${athleteId}`);
      renderSessions(allSessions);
    } catch {
      showToast("⚠️ Failed to load gym sessions", "error");
    }
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, (m) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[m])
    );
  }

  function showToast(msg, type = "success") {
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => (toast.style.opacity = "1"), 100);
    setTimeout(() => {
      toast.style.opacity = "0";
      setTimeout(() => toast.remove(), 400);
    }, 2500);
  }

  loadAthleteName();
  loadSessions();
})();

document.querySelectorAll(".tab-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
    btn.classList.add("active");
    document.getElementById(btn.dataset.tab).classList.add("active");
  });
});
