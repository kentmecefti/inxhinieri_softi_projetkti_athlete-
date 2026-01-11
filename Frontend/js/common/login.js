const API = window.env?.API || "http://localhost:8080/api";

document.getElementById("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value.trim();

  if (!username || !password) {
    alert("Please enter both username and password.");
    return;
  }

  try {
    const res = await fetch(`${API}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    if (!res.ok) {
      document.getElementById("forgotMsg").style.display = "block";

      const msg = await res.text();
      throw new Error(msg || "Invalid credentials");
    }

    document.getElementById("forgotMsg").style.display = "none";

    let token = await res.text();
    token = token.replace(/^"+|"+$/g, "");
    localStorage.setItem("token", token);
    localStorage.setItem("username", username);

    console.log("✅ Token stored:", token);

    const userRes = await fetch(`${API}/users/by-username/${username}`, {
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      }
    });

    if (!userRes.ok) throw new Error("User not found");

    const user = await userRes.json();
    console.log("👤 USER:", user);

    const userId = user.id;
    const roles = (user.roles || []).map(r => r.name?.toUpperCase() || "");

    localStorage.setItem("userId", userId);
    localStorage.setItem("userRoles", JSON.stringify(roles));

    console.log("🎭 Roles:", roles);

    if (roles.includes("ADMIN") || roles.includes("DATA ANALYST")) {
      localStorage.removeItem("athleteId");
      localStorage.removeItem("coachId");

      window.location.href = `../all-users.html?userId=${userId}`;
      return;
    }

    if (roles.includes("COACH")) {
      const coachRes = await fetch(`${API}/coaches/by-user/${userId}`, {
        headers: { "Authorization": `Bearer ${token}` }
      });

      if (!coachRes.ok) throw new Error("Coach record not found");

      const coach = await coachRes.json();
      console.log("🎓 Coach:", coach);

      localStorage.setItem("coachId", coach.id);
      localStorage.removeItem("athleteId");

      window.location.href = `../personal-coach.html?coachId=${coach.id}`;
      return;
    }

    if (roles.includes("ATHLETE")) {
      const athleteRes = await fetch(`${API}/athletes/by-user/${userId}`, {
        headers: { "Authorization": `Bearer ${token}` }
      });

      if (!athleteRes.ok) throw new Error("Athlete record not found");

      const athlete = await athleteRes.json();
      console.log("🏃 Athlete:", athlete);

      localStorage.setItem("athleteId", athlete.id);
      localStorage.removeItem("coachId");

      window.location.href = `../personal-athlete.html?athleteId=${athlete.id}`;
      return;
    }

    alert("Unknown role — cannot redirect.");

  } catch (err) {
    console.error("❌ Login error:", err);
    alert(`❌ Login failed: ${err.message}`);
    localStorage.removeItem("token");
  }
});

document.getElementById("togglePassword").addEventListener("click", function () {
    const input = document.getElementById("password");

    if (input.type === "password") {
        input.type = "text";
        this.classList.remove("fa-eye");
        this.classList.add("fa-eye-slash");
    } else {
        input.type = "password";
        this.classList.remove("fa-eye-slash");
        this.classList.add("fa-eye");
    }
});
