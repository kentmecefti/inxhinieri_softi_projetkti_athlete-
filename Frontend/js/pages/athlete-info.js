
const API = window.env?.API || "http://localhost:8080/api";

function decodeJwt(token) {
  try {
    const payload = token.split(".")[1];
    const base = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = atob(base);
    return JSON.parse(decoded);
  } catch {
    return null;
  }
}

const token = localStorage.getItem("token");
const decoded = decodeJwt(token);

const params = new URLSearchParams(location.search);
const ATHLETE_ID = params.get("athleteId");

function getAuthHeaders() {
  const h = { "Content-Type": "application/json" };
  if (token) h["Authorization"] = `Bearer ${token}`;
  return h;
}

let form, msgEl;

async function loadAthlete() {
  if (!ATHLETE_ID) {
    msgEl.textContent = "❌ Missing athleteId parameter!";
    msgEl.style.color = "orange";
    return;
  }

  try {
    const res = await fetch(`${API}/athletes/${ATHLETE_ID}`, {
      headers: getAuthHeaders(),
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    const a = await res.json();
    console.log("ATHLETE:", a);

    let usernameValue = "Unknown";
    let emailValue = "Unavailable";

    const athleteUserId =
      a.userId || a.user_id || a.user?.id || null;

    if (athleteUserId) {
      const userReq = await fetch(`${API}/users/${athleteUserId}`, {
        headers: getAuthHeaders(),
      });

      if (userReq.ok) {
        const user = await userReq.json();
        console.log("USER DATA:", user);

        if (user.username) usernameValue = user.username;
        if (user.email) emailValue = user.email;
      }
    }

    if (decoded && decoded.userId == athleteUserId) {
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

    document.getElementById("name").value = a.name || "";
    document.getElementById("lastname").value = a.lastname || "";
    document.getElementById("gender").value = a.gender || "";
    document.getElementById("birthDate").value = a.birthDate || "";
    document.getElementById("age").value = a.age || "";
    document.getElementById("athWeight").value = a.athWeight || "";
    document.getElementById("athHeight").value = a.athHeight || "";
    document.getElementById("category").value = a.category || "";
    document.getElementById("performance").value = a.performance || "";
    document.getElementById("club").value = a.club || "";
    document.getElementById("country").value = a.country || "";
    document.getElementById("city").value = a.city || "";

  } catch (err) {
    console.error(err);
    msgEl.textContent = "⚠️ Failed to load athlete info.";
    msgEl.style.color = "red";
  }
}

async function updateAthlete(e) {
  e.preventDefault();

  const body = {
    name: document.getElementById("name").value.trim(),
    lastname: document.getElementById("lastname").value.trim(),
    gender: document.getElementById("gender").value,
    birthDate: document.getElementById("birthDate").value || null,
    age: Number(document.getElementById("age").value) || null,
    athWeight: Number(document.getElementById("athWeight").value) || null,
    athHeight: Number(document.getElementById("athHeight").value) || null,
    category: document.getElementById("category").value.trim(),
    performance: document.getElementById("performance").value.trim(),
    club: document.getElementById("club").value.trim(),
    country: document.getElementById("country").value.trim(),
    city: document.getElementById("city").value.trim(),
  };

  try {
    const res = await fetch(`${API}/athletes/${ATHLETE_ID}`, {
      method: "PUT",
      headers: getAuthHeaders(),
      body: JSON.stringify(body),
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}`);

    msgEl.textContent = "✅ Athlete updated successfully!";
    msgEl.style.color = "limegreen";

  } catch (err) {
    console.error(err);
    msgEl.textContent = "❌ Update failed.";
    msgEl.style.color = "red";
  }
}

async function changePassword() {
  const currentPass = document.getElementById("oldPass").value.trim();
  const newPass = document.getElementById("newPass").value.trim();

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
        newPassword: newPass
      }),
    });

    const text = await res.text();

    if (!res.ok) {
      throw new Error(text);
    }

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
  form = document.getElementById("athleteForm");
  msgEl = document.getElementById("msg");

  loadAthlete();

  form?.addEventListener("submit", updateAthlete);

  document.getElementById("openPassModal").onclick = () =>
    (document.getElementById("passModal").style.display = "block");

  document.getElementById("closePassModal").onclick = () =>
    (document.getElementById("passModal").style.display = "none");

  document.getElementById("savePasswordBtn").onclick = changePassword;
});
