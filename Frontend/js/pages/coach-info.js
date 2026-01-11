
const API = window.env?.API || "http://localhost:8080/api";

function decodeJwt(token) {
  if (!token) return null;
  try {
    const payload = token.split(".")[1];
    const base = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base));
  } catch {
    return null;
  }
}

const token = localStorage.getItem("token");
const decoded = decodeJwt(token);

const params = new URLSearchParams(location.search);
const COACH_ID = params.get("coachId");

function getAuthHeaders() {
  const h = { "Content-Type": "application/json" };
  if (token) h.Authorization = `Bearer ${token}`;
  return h;
}

let form, msgEl;

async function loadCoach() {
  if (!COACH_ID) {
    msgEl.textContent = "❌ Missing coachId parameter!";
    msgEl.style.color = "orange";
    return;
  }

  try {
    const res = await fetch(`${API}/coaches/${COACH_ID}`, {
      headers: getAuthHeaders(),
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const c = await res.json();
    console.log("COACH:", c);

    let usernameValue = "Unknown";
    let emailValue = "Unavailable";

    const coachUserId = c.userId || c.user_id || c.user?.id || null;

    if (coachUserId) {
      const userReq = await fetch(`${API}/users/${coachUserId}`, {
        headers: getAuthHeaders(),
      });

      if (userReq.ok) {
        const user = await userReq.json();
        usernameValue = user.username || usernameValue;
        emailValue = user.email || emailValue;
      }
    }

    if (decoded && decoded.userId == coachUserId) {
      if (decoded.sub) usernameValue = decoded.sub;
      if (decoded.email) emailValue = decoded.email;
    }

    const usernameField = document.getElementById("username");
    if (usernameField) {
      usernameField.value = usernameValue;
      usernameField.readOnly = true;
      usernameField.style.background = "#eee";
      usernameField.style.cursor = "not-allowed";
    }

    const emailField = document.getElementById("email");
    if (emailField) {
      emailField.value = emailValue;
      emailField.readOnly = true;
      emailField.style.background = "#eee";
      emailField.style.cursor = "not-allowed";
    }

    document.getElementById("name").value = c.name || "";
    document.getElementById("lastname").value = c.lastname || "";
    document.getElementById("gender").value = c.gender || "";
    document.getElementById("experienceYears").value = c.experienceYears || "";
    document.getElementById("specialization").value = c.specialization || "";
    document.getElementById("phone").value = c.phone || "";
    document.getElementById("club").value = c.club || "";
    document.getElementById("country").value = c.country || "";

  } catch (err) {
    console.error(err);
    msgEl.textContent = "⚠️ Failed to load coach info.";
    msgEl.style.color = "red";
  }
}

async function updateCoach(e) {
  e.preventDefault();

  const body = {
    name: document.getElementById("name").value.trim(),
    lastname: document.getElementById("lastname").value.trim(),
    gender: document.getElementById("gender").value.trim(),
    experienceYears: Number(document.getElementById("experienceYears").value) || 0,
    specialization: document.getElementById("specialization").value.trim(),
    phone: document.getElementById("phone").value.trim(),
    club: document.getElementById("club").value.trim(),
    country: document.getElementById("country").value.trim(),
  };

  try {
    const res = await fetch(`${API}/coaches/${COACH_ID}`, {
      method: "PUT",
      headers: getAuthHeaders(),
      body: JSON.stringify(body),
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    msgEl.textContent = "✅ Coach updated successfully!";
    msgEl.style.color = "limegreen";

  } catch (err) {
    console.error(err);
    msgEl.textContent = "❌ Update failed.";
    msgEl.style.color = "red";
  }
}

async function changePassword() {
  const currentPass = document.getElementById("oldPass")?.value.trim();
  const newPass = document.getElementById("newPass")?.value.trim();

  if (!currentPass || !newPass) {
    alert("⚠️ Please fill both password fields.");
    return;
  }

  try {
    const res = await fetch(`${API}/auth/change-password`, {
      method: "PUT",
      headers: getAuthHeaders(),
      body: JSON.stringify({
        currentPassword: currentPass,
        newPassword: newPass,
      }),
    });

    const text = await res.text();
    if (!res.ok) throw new Error(text);

    alert(text);
    document.getElementById("passModal").style.display = "none";
    document.getElementById("oldPass").value = "";
    document.getElementById("newPass").value = "";

  } catch (err) {
    console.error(err);
    alert(err.message || "❌ Password change failed.");
  }
}

document.addEventListener("DOMContentLoaded", () => {
  form = document.getElementById("coachForm");
  msgEl = document.getElementById("msg");

  loadCoach();

  form?.addEventListener("submit", updateCoach);

  document.getElementById("openPassModal")?.addEventListener("click", () => {
    document.getElementById("passModal").style.display = "block";
  });

  document.getElementById("closePassModal")?.addEventListener("click", () => {
    document.getElementById("passModal").style.display = "none";
  });

  document.getElementById("savePasswordBtn")?.addEventListener("click", changePassword);
});
