
const API = window.env?.API || "http://localhost:8080/api";
const state = { all: [], roleFilter: "all", statusFilter: "all", q: "" };

function getAuthHeaders(extra = {}) {
  const token = localStorage.getItem("token");
  const headers = { ...extra };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  return headers;
}

async function loadUsers() {
  try {
    clearErr();

    const users = await fetchJson(`${API}/users`);
    const [athletes, coaches] = await Promise.all([
      fetchJson(`${API}/athletes`),
      fetchJson(`${API}/coaches`)
    ]);

    const combined = [];

    for (const a of athletes || []) {
      const userId = a.user?.id ?? a.userId ?? null;
      const user = users.find(u => u.id === userId);
      combined.push({
        id: userId,
        athleteId: a.id,
        name: a.name,
        role: "athlete",
        active: user?.active ?? false
      });
    }

    for (const c of coaches || []) {
      const userId = c.user?.id ?? c.userId ?? null;
      const user = users.find(u => u.id === userId);
      combined.push({
        id: userId,
        coachId: c.id,
        name: c.name,
        role: "coach",
        active: user?.active ?? false
      });
    }

    state.all = combined;
    updateKpis();
    render();

  } catch (err) {
    showErr(err);
    state.all = [];
    updateKpis();
    render();
  }
}

function updateKpis() {
  document.getElementById("kpiAll").textContent = state.all.length;
  document.getElementById("kpiAth").textContent = state.all.filter(u => u.role === "athlete").length;
  document.getElementById("kpiCoach").textContent = state.all.filter(u => u.role === "coach").length;
  document.getElementById("kpiActive").textContent = state.all.filter(u => u.active).length;
  document.getElementById("kpiInactive").textContent = state.all.filter(u => !u.active).length;
}

function render() {
  const filtered = state.all.filter(u => {
    if (state.roleFilter !== "all" && u.role !== state.roleFilter) return false;
    if (state.statusFilter === "active" && !u.active) return false;
    if (state.statusFilter === "inactive" && u.active) return false;
    if (state.q && !u.name.toLowerCase().includes(state.q)) return false;
    return true;
  });

  document.getElementById("empty").style.display = filtered.length ? "none" : "block";

  let dt;
  if ($.fn.DataTable.isDataTable("#userTable")) {
    dt = $("#userTable").DataTable();
    dt.clear();
  } else {
    dt = $("#userTable").DataTable({
      pageLength: 10,
      lengthChange: false,
      ordering: true,
      searching: false,
      info: false,
      autoWidth: false
    });
  }

  for (const u of filtered) {

    const openBtn =
      u.role === "athlete"
        ? `<button class="btn small blue" onclick="openAthlete(${u.athleteId})">Open</button>`
        : `<button class="btn small green" onclick="openCoach(${u.coachId})">Open</button>`;

    const statusBadge = `
      <span class="badge active" style="opacity:${u.active ? 1 : 0.3};">Active</span>
      <span class="badge inactive" style="opacity:${u.active ? 0.3 : 1};">Inactive</span>
    `;

    let actionBtns = openBtn;

    if (u.id) {
      if (!u.active) {
        actionBtns += `
          <button class="btn small success" onclick="toggleActive(${u.id}, true)">Activate</button>
        `;
      } else {
        actionBtns += `
          <button class="btn small danger" onclick="toggleActive(${u.id}, false)">Deactivate</button>
        `;
      }
    } else {
      actionBtns += ` <button class="btn small muted" disabled>No User</button>`;
    }

    const roleBadge =
      u.role === "athlete"
        ? `<span class="badge role-athlete">athlete</span>`
        : `<span class="badge role-coach">coach</span>`;

    const rowData = [
      u.id ?? "-",
      escapeHtml(u.name),
      roleBadge,
      statusBadge,
      actionBtns
    ];

    const newRow = dt.row.add(rowData);
    const tr = $(newRow.node());

    tr.addClass(u.role === "athlete" ? "role-athlete" : "role-coach");

    tr.css({
      opacity: u.active ? 1 : 0.6,
      backgroundColor: u.active ? "transparent" : "rgba(255,255,255,0.05)"
    });
  }

  dt.draw(false);
}

async function toggleActive(id, active) {
  const action = active ? "activate" : "deactivate";
  if (!confirm(`Are you sure you want to ${action} this user?`)) return;

  try {
    const endpoint = `${API}/users/${id}/${action}`;
    const res = await fetch(endpoint, {
      method: "PUT",
      headers: getAuthHeaders({ "Content-Type": "application/json" })
    });
    const text = await res.text();

    if (res.ok) {
      alert(`✅ User ${action}d successfully!`);
      await loadUsers();
    } else {
      alert(`❌ Failed to ${action} user (${res.status}): ${text}`);
    }
  } catch (err) {
    alert(`⚠️ Error trying to ${action} user.`);
    console.error(err);
  }
}

function openAthlete(id) {
  if (!id) return alert("⚠️ Invalid athlete ID");
  location.href = `personal-athlete.html?athleteId=${id}`;
}

function openCoach(id) {
  if (!id) return alert("⚠️ Invalid coach ID");
  location.href = `personal-coach.html?coachId=${id}`;
}

async function fetchJson(url) {
  const r = await fetch(url, { headers: getAuthHeaders() });
  if (!r.ok) throw new Error("HTTP " + r.status);
  return r.json();
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, m =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[m])
  );
}

function showErr(e) {
  const box = document.getElementById("err");
  box.style.display = "block";
  box.textContent = "⚠️ " + (e.message || e);
}

function clearErr() {
  const box = document.getElementById("err");
  box.style.display = "none";
  box.textContent = "";
}

document.addEventListener("DOMContentLoaded", () => {

  document.querySelectorAll("#roleTabs .tab").forEach(tab => {
    tab.addEventListener("click", () => {
      document.querySelectorAll("#roleTabs .tab").forEach(t => t.classList.remove("active"));
      tab.classList.add("active");
      state.roleFilter = tab.dataset.filter;
      render();
    });
  });

  document.querySelectorAll("#statusTabs .tab").forEach(tab => {
    tab.addEventListener("click", () => {
      document.querySelectorAll("#statusTabs .tab").forEach(t => t.classList.remove("active"));
      tab.classList.add("active");
      state.statusFilter = tab.dataset.filter;
      render();
    });
  });

  document.getElementById("search").addEventListener("input", e => {
    state.q = e.target.value.toLowerCase().trim();
    render();
  });

  loadUsers();
});
