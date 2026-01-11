const API = window.env?.API || "http://localhost:8080/api";
const ATHLETE_ID =
  new URLSearchParams(location.search).get("athleteId") ||
  localStorage.getItem("athleteId");

function getAuthHeaders() {
  const token = localStorage.getItem("token");
  if (!token) {
    alert("⚠️ You are not logged in!");
    window.location.href = "../login.html";
    return {};
  }
  return {
    "Authorization": `Bearer ${token}`,
    "Content-Type": "application/json"
  };
}

async function loadRequests() {
  try {
    const res = await fetch(
      `${API}/athletes/${ATHLETE_ID}/coaches/decision/pending`,
      { headers: getAuthHeaders() }
    );

    if (!res.ok) throw new Error(`Failed to load requests`);

    const data = await res.json();
    const body = document.getElementById("requestBody");
    body.innerHTML = "";

    if (data.length === 0) {
      body.innerHTML = `<tr><td colspan="3">No requests found.</td></tr>`;
      return;
    }

    data.forEach(req => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${req.name} ${req.lastname || ""}</td>
        <td class="status">${req.status}</td>
        <td>
          ${
            req.status === "pending"
              ? `
                <button class="btn btn-accept" onclick="handleRequest(${req.coach_id}, 'accept')">Accept</button>
                <button class="btn btn-refuse" onclick="handleRequest(${req.coach_id}, 'refuse')">Refuse</button>
                `
              : `<button class="btn btn-disabled" disabled>${req.status}</button>`
          }
        </td>
      `;
      body.appendChild(tr);
    });

  } catch (err) {
    showToast("⚠️ Could not load requests", "error");
  }
}

async function handleRequest(coachId, action) {
  const endpoint = action === "accept" ? "accept" : "refuse";

  try {
    const res = await fetch(
      `${API}/relations/${endpoint}?athleteId=${ATHLETE_ID}&coachId=${coachId}`,
      { method: "POST", headers: getAuthHeaders() }
    );

    if (!res.ok) throw new Error(`Failed action`);

    const msg = await res.text();
    showToast(`✅ ${msg}`);

    loadRequests();
    loadAcceptedCoaches();

    if (action === "accept") {
      window.parent.postMessage("refreshCoaches", "*");
    }

  } catch (err) {
    showToast("❌ Could not update request", "error");
  }
}

async function loadAcceptedCoaches() {
  try {
    const res = await fetch(
      `${API}/athletes/${ATHLETE_ID}/coaches/decision/accept`,
      { headers: getAuthHeaders() }
    );

    if (!res.ok) throw new Error();

    const coaches = await res.json();
     const body = document.getElementById("acceptedBody");
        body.innerHTML = "";
         if(coaches.length === 0){
           body.innerHTML = `<tr><td colspan="3">No requests found.</td></tr>`;
          return;
         }else{
         renderAcceptedCoaches(coaches);
         }

  } catch (err) {
    showToast("⚠️ Failed to load accepted coaches", "error");
  }
}

function renderAcceptedCoaches(coaches) {
  const body = document.getElementById("acceptedBody");

  if (!coaches.length) {
    body.innerHTML = `<tr><td colspan="2">No accepted coaches.</td></tr>`;
  } else {
    body.innerHTML = coaches
      .map(
        c => `
        <tr>
          <td>${c.name} ${c.lastname || ""}</td>
          <td>
            <button class="unlink-btn" onclick="unlinkCoach(${c.coach_id})">
              ❌ Remove
            </button>
          </td>
        </tr>
      `
      )
      .join("");
  }

  if ($.fn.DataTable.isDataTable("#acceptedTable")) {
    $("#acceptedTable").DataTable().destroy();
  }

  $("#acceptedTable").DataTable({
    paging: true,
    searching: true,
    info: false,
    lengthChange: false,
    pageLength: 5,
    columnDefs: [
      { orderable: false, targets: 1 }
    ]
  });
}

async function unlinkCoach(coachId) {
  if (!confirm("Are you sure you want to unlink this coach?")) return;

  try {
    const res = await fetch(
      `${API}/relations/unlink?coachId=${coachId}&athleteId=${ATHLETE_ID}`,
      {
        method: "DELETE",
        headers: getAuthHeaders()
      }
    );

    if (!res.ok) throw new Error();

    showToast("✔ Coach removed");
    loadAcceptedCoaches();
    window.parent.postMessage("refreshCoaches", "*");

  } catch (err) {
    showToast("❌ Failed to unlink coach", "error");
  }
}

function showToast(message, type = "success") {
  const toast = document.createElement("div");
  toast.textContent = message;
  toast.style.cssText = `
    position: fixed; bottom: 20px; right: 20px;
    background: ${type === "error" ? "#e74c3c" : "#2d9f25"};
    color: #fff; padding: 10px 16px;
    border-radius: 8px; z-index: 9999;
    opacity: 0; transition: 0.3s;
  `;
  document.body.appendChild(toast);
  setTimeout(() => (toast.style.opacity = 1), 50);
  setTimeout(() => {
    toast.style.opacity = 0;
    setTimeout(() => toast.remove(), 400);
  }, 2500);
}

document.addEventListener("DOMContentLoaded", () => {
  loadRequests();
  loadAcceptedCoaches();
});
