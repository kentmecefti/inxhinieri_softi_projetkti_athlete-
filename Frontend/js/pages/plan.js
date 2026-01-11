
const API = window.env?.API || "http://localhost:8080/api";

const params = new URLSearchParams(location.search);
const ATHLETE_ID = params.get("athleteId") ? Number(params.get("athleteId")) : null;
const COACH_ID   = params.get("coachId")   ? Number(params.get("coachId"))   : null;

const IS_PREVIEW_MODE = !!(ATHLETE_ID && COACH_ID && params.get("readonly") === "true");
const IS_ATHLETE_MODE = !!(ATHLETE_ID && !COACH_ID);
const IS_COACH_MODE   = !!(COACH_ID && !IS_PREVIEW_MODE);

const token = localStorage.getItem("token");

function authHeaders(extra = {}) {
  const headers = { "Content-Type": "application/json", ...extra };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  return headers;
}

function ensureAuth() {
  if (!token) {
    alert("You are not logged in. Please log in again.");
    window.location.href = "../../login/login.html";
    throw new Error("Missing token");
  }
}

const titleEl        = document.getElementById("athleteTitle");
const weekPickerEl   = document.getElementById("weekPicker");
const theadRow       = document.getElementById("theadRow");
const tbody          = document.getElementById("planTableBody");
const toastEl        = document.getElementById("toast");
const backBtn        = document.getElementById("backBtn");
const sendPredBtn    = document.getElementById("sendPredBtn");
const sendActBtn     = document.getElementById("sendActBtn");
const planActions    = document.querySelector(".plan-actions");
const loadingOverlay = document.getElementById("loadingOverlay");

const toggleBtn         = document.getElementById("toggleAthletesBtn");
const dropdownContainer = document.getElementById("athleteDropdown");
const athleteListEl     = document.getElementById("athleteList");
const athleteSearchEl   = document.getElementById("athleteSearch");

let existingByKey    = new Map(); 
let currentWeekDates = [];
let allAthletes      = [];
let selectedRows     = new Set();
let HAS_COACH        = false;  

const WEEKDAY_NAMES = ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"];

const keyOf = (dateYmd, athleteId) => `${dateYmd}|${athleteId ?? ""}`;

function fmtDateYYYYMMDD(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function mondayOfISOWeek(isoWeekStr) {
  const [yStr, wStr] = isoWeekStr.split("-W");
  const year = +yStr, week = +wStr;
  const jan4 = new Date(year, 0, 4);
  const dayOfWeek = jan4.getDay() || 7;
  const monday = new Date(jan4);
  monday.setDate(jan4.getDate() - (dayOfWeek - 1) + (week - 1) * 7);
  return monday;
}

function getCurrentISOWeek() {
  const d = new Date(), tmp = new Date(d);
  tmp.setDate(d.getDate() + 4 - ((d.getDay() || 7)));
  const yearStart = new Date(tmp.getFullYear(), 0, 1);
  const weekNo = Math.ceil((((tmp - yearStart) / 86400000) + 1) / 7);
  return `${tmp.getFullYear()}-W${String(weekNo).padStart(2, "0")}`;
}

function buildWeekDatesFromMonday(monday) {
  return Array.from({ length: 7 }, (_, i) =>
    new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + i)
  );
}

function esc(s) {
  return String(s ?? "")
    .replace(/&/g,"&amp;")
    .replace(/</g,"&lt;")
    .replace(/>/g,"&gt;")
    .replace(/"/g,"&quot;")
    .replace(/'/g,"&#39;");
}

function toast(msg, type = "info") {
  if (!toastEl) return alert(msg);
  toastEl.textContent = msg;
  toastEl.style.background = type === "error" ? "#e74c3c" : "#2563eb";
  toastEl.classList.add("show");
  clearTimeout(toastEl._t);
  toastEl._t = setTimeout(() => toastEl.classList.remove("show"), 2000);
}

function showLoading(msg = "Processing...") {
  if (!loadingOverlay) return;
  loadingOverlay.querySelector("p").textContent = msg;
  loadingOverlay.classList.remove("hidden");
}
function hideLoading() {
  if (!loadingOverlay) return;
  loadingOverlay.classList.add("hidden");
}


async function loadCoachPlans() {
  ensureAuth();
  if (!COACH_ID) return;
  const res = await fetch(`${API}/plans/coach/${COACH_ID}`, { headers: authHeaders() });
  if (!res.ok) throw new Error("Failed to load coach plans");
  const data = await res.json();

  existingByKey.clear();
  data.forEach(r => existingByKey.set(keyOf(r.planDate, r.athleteId), r));
}


async function loadAthletePlans() {
  ensureAuth();
  if (!ATHLETE_ID) return;

  const plansRes = await fetch(`${API}/plans/athlete/${ATHLETE_ID}`, { headers: authHeaders() });
  if (!plansRes.ok) throw new Error("Failed to load athlete plans");
  const plans = await plansRes.json();

  existingByKey.clear();
  plans.forEach(r => existingByKey.set(keyOf(r.planDate, r.athleteId), r));

  HAS_COACH = false;
  try {
    const relRes = await fetch(`${API}/athletes/${ATHLETE_ID}/coaches/decision/accept`, {
      headers: authHeaders()
    });

    if (relRes.ok) {
      const relData = await relRes.json();
      HAS_COACH = Array.isArray(relData) && relData.length > 0;
    } else {
      HAS_COACH = false;
    }
  } catch (err) {
    console.warn("Relation check failed:", err);
    HAS_COACH = false; // fallback: treat as solo
  }
}


async function loadCoachAthletes() {
  ensureAuth();
  if (!COACH_ID) return;

  const res = await fetch(`${API}/coaches/${COACH_ID}`, { headers: authHeaders() });
  if (!res.ok) throw new Error("Failed to load coach info");

  const coachData = await res.json();

  allAthletes = (coachData.athletesWithStatus || [])
    .filter(a => (a.relation_status || "").toLowerCase() === "accept")
    .sort((a, b) => (a.name || "").localeCompare(b.name || ""));
}


function renderAthleteDropdown(filter = "") {
  if (!athleteListEl) return;

  athleteListEl.innerHTML = "";

  const filtered = allAthletes.filter(a =>
    (a.name || "").toLowerCase().includes(filter.toLowerCase())
  );

  const allBtn = document.createElement("div");
  allBtn.className = "dropdown-item";
  allBtn.dataset.id = "ALL";
  allBtn.textContent = "📡 All Athletes";
  athleteListEl.appendChild(allBtn);

  filtered.forEach(a => {
    const div = document.createElement("div");
    div.className = "dropdown-item";
    div.dataset.id = a.athlete_id || a.id;
    div.textContent = a.name;
    athleteListEl.appendChild(div);
  });
}

athleteSearchEl?.addEventListener("input", e => {
  renderAthleteDropdown(e.target.value);
});

athleteListEl?.addEventListener("click", e => {
  const item = e.target.closest(".dropdown-item");
  if (!item) return;

  if (item.dataset.id === "ALL") {
    const newState = item.classList.toggle("selected");
    athleteListEl.querySelectorAll(".dropdown-item").forEach(btn => {
      if (btn !== item) btn.classList.toggle("selected", newState);
    });
  } else {
    item.classList.toggle("selected");
    athleteListEl.querySelector('.dropdown-item[data-id="ALL"]')?.classList.remove("selected");
  }
});

function selectedAthleteId() {
  if (!athleteListEl) return null;
  const selected = athleteListEl.querySelectorAll(".dropdown-item.selected");
  if (!selected.length || selected[0].dataset.id === "ALL") return null;
  return Number(selected[0].dataset.id);
}

function renderThead() {
  if (IS_COACH_MODE) {
    theadRow.innerHTML = `
      <th style="width:140px">Date</th>
      <th>Weekday</th>
      <th>Prediction Plan</th>
      <th>Actual Plan</th>
    `;

  } else {
    theadRow.innerHTML = `
      <th style="width:140px">Date</th>
      <th>Weekday</th>
      <th>Prediction Plan</th>
      <th>Actual Plan</th>
    `;
  }
}


function renderTable() {
  tbody.innerHTML = "";

  currentWeekDates.forEach((dateObj, idx) => {
    const ymd = fmtDateYYYYMMDD(dateObj);
    const weekday = WEEKDAY_NAMES[idx];

    const aid = IS_COACH_MODE ? (selectedAthleteId() || ATHLETE_ID) : ATHLETE_ID;
    const ex  = existingByKey.get(keyOf(ymd, aid));

    const tr = document.createElement("tr");
    tr.dataset.date = ymd;

    if (IS_COACH_MODE) {
      tr.innerHTML = `
    <td><div class="muted">${ymd}</div></td>
    <td><strong>${weekday}</strong></td>
    <td>
      <textarea class="pred-input" data-date="${ymd}" rows="3"
        placeholder="Planned work...">${ex?.predictionPlan ? esc(ex.predictionPlan) : ""}</textarea>
    </td>
    <td>
      <textarea class="act-input" data-date="${ymd}" rows="3"
        placeholder="Actual work...">${ex?.actualPlan ? esc(ex.actualPlan) : ""}</textarea>
    </td>
  `;

      tr.addEventListener("click", e => {
        // prevent selecting when typing
        if (e.target.tagName === "TEXTAREA") return;

        const date = ymd;

        if (selectedRows.has(date)) {
          selectedRows.delete(date);
          tr.classList.remove("row-selected");
        } else {
          selectedRows.add(date);
          tr.classList.add("row-selected");
        }
      });

      tbody.appendChild(tr);
      return;
    }

    if (IS_ATHLETE_MODE) {
      if (HAS_COACH || IS_PREVIEW_MODE) {
        tr.innerHTML = `
          <td><div class="muted">${ymd}</div></td>
          <td><strong>${weekday}</strong></td>
          <td>
            <div class="cell-text">
              ${ex?.predictionPlan ? esc(ex.predictionPlan) : "<em>No prediction</em>"}
            </div>
          </td>
          <td>
            <div class="cell-text">
              ${ex?.actualPlan ? esc(ex.actualPlan) : "<em>No actual yet</em>"}
            </div>
          </td>
        `;
      } else {
        tr.innerHTML = `
          <td><div class="muted">${ymd}</div></td>
          <td><strong>${weekday}</strong></td>
          <td>
            <textarea class="pred-input" data-date="${ymd}" rows="3" placeholder="Planned work...">${
              ex?.predictionPlan ?? ""
            }</textarea>
          </td>
          <td>
            <textarea class="act-input" data-date="${ymd}" rows="3" placeholder="Actual work...">${
              ex?.actualPlan ?? ""
            }</textarea>
          </td>
        `;
      }
      tbody.appendChild(tr);
      return;
    }

    tr.innerHTML = `
      <td><div class="muted">${ymd}</div></td>
      <td><strong>${weekday}</strong></td>
      <td>${ex?.predictionPlan ? esc(ex.predictionPlan) : "<em>No prediction</em>"}</td>
      <td>${ex?.actualPlan ? esc(ex.actualPlan) : "<em>No actual</em>"}</td>
    `;
    tbody.appendChild(tr);
  });
}


async function sendPlans(isPrediction) {
  ensureAuth();
  if (!IS_COACH_MODE) return toast("Read-only mode.", "error");

  const selectedBtns = athleteListEl?.querySelectorAll(".dropdown-item.selected") || [];
  let athleteIds = [];

  if (ATHLETE_ID && !selectedBtns.length) {
    athleteIds = [ATHLETE_ID];
  } else if ([...selectedBtns].some(b => b.dataset.id === "ALL")) {
    athleteIds = allAthletes.map(a => a.athlete_id || a.id);
  } else {
    athleteIds = [...selectedBtns].map(b => Number(b.dataset.id));
  }

  if (!athleteIds.length) return toast("⚠️ Select at least one athlete", "error");
  if (!selectedRows.size) return toast("⚠️ Select at least one day", "error");

  try {
    showLoading(isPrediction ? "Sending predictions..." : "Updating actuals...");

    for (const date of selectedRows) {
      const pred = document.querySelector(`.pred-input[data-date="${date}"]`)?.value || "";
      const act  = document.querySelector(`.act-input[data-date="${date}"]`)?.value  || "";

      if (isPrediction) {
        const payload = {
          athleteIds,
          planDate: date,
          predictionPlan: pred,
          actualPlan: "",
          notes: null
        };
        const res = await fetch(`${API}/plans/coach/${COACH_ID}/send`, {
          method: "POST",
          headers: authHeaders(),
          body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error("Failed to send prediction");
      } else {
        for (const aid of athleteIds) {
          const ex = existingByKey.get(keyOf(date, aid));
          if (!ex?.id) continue;

          const payload = {
            athleteId: ex.athleteId,
            coachId: ex.coachId,
            planDate: date,
            predictionPlan: ex.predictionPlan,
            actualPlan: act,
            notes: ex.notes ?? null
          };
          const res = await fetch(`${API}/plans/${ex.id}`, {
            method: "PUT",
            headers: authHeaders(),
            body: JSON.stringify(payload)
          });
          if (!res.ok) throw new Error("Failed to update actual");
          ex.actualPlan = act;
          existingByKey.set(keyOf(date, aid), ex);
        }
      }
    }

    toast(isPrediction ? "✅ Predictions sent" : "✅ Actuals updated");

    if (isPrediction) {
      await loadCoachPlans();
      renderTable();
    }
  } catch (err) {
    console.error(err);
    toast("⚠️ " + err.message, "error");
  } finally {
    hideLoading();
  }
}


async function soloSendPlans(isPrediction) {
  if (!IS_ATHLETE_MODE || HAS_COACH) return; 
  ensureAuth();

  try {
    showLoading(isPrediction ? "Saving prediction..." : "Saving actual...");

    for (const dateObj of currentWeekDates) {
      const ymd = fmtDateYYYYMMDD(dateObj);

      const predEl = document.querySelector(`.pred-input[data-date="${ymd}"]`);
      const actEl  = document.querySelector(`.act-input[data-date="${ymd}"]`);

      const pred   = predEl?.value.trim() || "";
      const actual = actEl?.value.trim() || "";

      if (!pred && !actual) continue;

      const existing = existingByKey.get(keyOf(ymd, ATHLETE_ID));

      if (!existing) {
        const res = await fetch(`${API}/plans`, {
          method: "POST",
          headers: authHeaders(),
          body: JSON.stringify({
            athleteId: ATHLETE_ID,
            coachId: null, 
            planDate: ymd,
            predictionPlan: pred,
            actualPlan: actual,
            notes: null
          })
        });
        if (!res.ok) {
          const msg = await res.text().catch(() => "");
          throw new Error(msg || "Failed to create plan");
        }
        continue;
      }

      const res = await fetch(`${API}/plans/${existing.id}`, {
        method: "PUT",
        headers: authHeaders(),
        body: JSON.stringify({
          athleteId: ATHLETE_ID,
          coachId: existing.coachId ?? null,
          planDate: ymd,
          predictionPlan: isPrediction ? pred : (existing.predictionPlan || ""),
          actualPlan: isPrediction ? (existing.actualPlan || "") : actual,
          notes: existing.notes ?? null
        })
      });

      if (!res.ok) {
        const msg = await res.text().catch(() => "");
        throw new Error(msg || "Failed to update plan");
      }
    }

    toast("✅ Plans saved!");
    await loadAthletePlans();
    renderTable();

  } catch (err) {
    console.error("Solo save error:", err);
    toast("⚠️ " + (err.message || "Solo save failed"), "error");
  } finally {
    hideLoading();
  }
}


async function init() {
  try {
    ensureAuth();

    const currentISO = getCurrentISOWeek();
    if (weekPickerEl) weekPickerEl.value = currentISO;

    if (toggleBtn) {
      if (IS_COACH_MODE) {
        toggleBtn.style.display = "";
        toggleBtn.addEventListener("click", () => {
          if (!dropdownContainer) return;
          dropdownContainer.style.display =
            dropdownContainer.style.display === "none" || !dropdownContainer.style.display
              ? "block"
              : "none";
        });
      } else {
        toggleBtn.style.display = "none";
      }
    }

    if (IS_COACH_MODE) {
      titleEl.textContent = ATHLETE_ID
        ? "🎓 Coach Editing Single Athlete"
        : "🎓 Coach Plan Manager";

      planActions.style.display = "";
      await Promise.all([loadCoachPlans(), loadCoachAthletes()]);
      renderAthleteDropdown();

      sendPredBtn?.addEventListener("click", () => sendPlans(true));
      sendActBtn?.addEventListener("click", () => sendPlans(false));

    } else if (IS_ATHLETE_MODE) {
      await loadAthletePlans();

      if (HAS_COACH) {
        titleEl.textContent = "🏃 Athlete Weekly Plan (Coach-managed)";
        planActions.style.display = "none";
      } else {
        titleEl.textContent = "🏃 Solo Athlete Weekly Plan (Editable)";
        planActions.style.display = "";
        sendPredBtn?.addEventListener("click", () => soloSendPlans(true));
        sendActBtn?.addEventListener("click", () => soloSendPlans(false));
      }

    } else if (IS_PREVIEW_MODE) {
      titleEl.textContent = "🏃 Athlete Weekly Plan (Preview)";
      planActions.style.display = "none";
      await loadAthletePlans();
    } else {
      throw new Error("Missing coachId or athleteId in URL");
    }

    renderThead();
    const monday = mondayOfISOWeek(currentISO);
    currentWeekDates = buildWeekDatesFromMonday(monday);
    renderTable();

    weekPickerEl?.addEventListener("change", e => {
      const monday2 = mondayOfISOWeek(e.target.value);
      currentWeekDates = buildWeekDatesFromMonday(monday2);
      selectedRows.clear();
      renderTable();
    });

    backBtn?.addEventListener("click", goBack);

    if (COACH_ID && !ATHLETE_ID && backBtn) {
      backBtn.style.visibility = "hidden";
      backBtn.style.pointerEvents = "none";
    }

  } catch (err) {
    console.error("Init failed:", err);
    toast("⚠️ Init failed: " + err.message, "error");
  }
}

function goBack() {
  if (document.referrer && document.referrer !== window.location.href) {
    window.history.back();
  } else {
    location.href = "../welcome.html";
  }
}

document.addEventListener("DOMContentLoaded", init);
