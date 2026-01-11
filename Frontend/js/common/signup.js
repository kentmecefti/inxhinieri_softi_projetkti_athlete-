
const API = window.env?.API || "http://localhost:8080/api";

document.getElementById("signupForm").addEventListener("submit", async (e) => {
  e.preventDefault();

  const username = document.getElementById("username").value.trim();
  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value.trim();
  const role =
      document.getElementById("role")?.value?.trim()?.toUpperCase() || "ATHLETE";

  if (!username || !email || !password) {
    alert("⚠️ Please fill in all fields.");
    return;
  }

  try {
    const regRes = await fetch(`${API}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, email, password, role })
    });

    const regText = await regRes.text();
    if (!regRes.ok) throw new Error(regText || "Registration failed");

    alert("✅ Registration successful! Logging in...");

    const loginRes = await fetch(`${API}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });

    if (!loginRes.ok) {
      throw new Error(await loginRes.text() || "Auto-login failed");
    }

    let token = await loginRes.text();
    token = token.replace(/^"+|"+$/g, ""); 

    localStorage.setItem("token", token);
    localStorage.setItem("username", username);
    const encodedUsername = encodeURIComponent(username);

    const userRes = await fetch(
        `${API}/users/by-username/${encodedUsername}`,
        {
          headers: {
            "Authorization": `Bearer ${token}`
          }
        }
    );

    if (!userRes.ok) {
      throw new Error("Could not fetch logged user info");
    }

    const user = await userRes.json();
    if (!user || !user.id) {
      throw new Error("Invalid user response");
    }

    const userId = user.id;
    localStorage.setItem("userId", userId);

    const roles = (user.roles || []).map(r =>
        (r.name || "")
            .replace("ROLE_", "")
            .trim()
            .toUpperCase()
    );

    console.log("🎭 User roles:", roles);
    localStorage.setItem("userRoles", JSON.stringify(roles));


    if (roles.includes("COACH")) {
      const coachRes = await fetch(
          `${API}/coaches/by-user/${userId}`,
          {
            headers: {
              "Authorization": `Bearer ${token}`
            }
          }
      );

      if (!coachRes.ok) {
        throw new Error("Coach record not found");
      }

      const coach = await coachRes.json();
      localStorage.setItem("coachId", coach.id);

      window.location.href = `./profile-entry.html?coachId=${coach.id}`;
      return;
    }

    if (roles.includes("ATHLETE")) {
      const athleteRes = await fetch(
          `${API}/athletes/by-user/${userId}`,
          {
            headers: {
              "Authorization": `Bearer ${token}`
            }
          }
      );

      if (!athleteRes.ok) {
        throw new Error("Athlete record not found");
      }

      const athlete = await athleteRes.json();
      localStorage.setItem("athleteId", athlete.id);

      window.location.href = `./profile-entry.html?athleteId=${athlete.id}`;
      return;
    }

    alert("⚠️ No valid role found for this user.");

  } catch (err) {
    console.error("❌ Signup error:", err);
    alert(`❌ Signup failed: ${err.message}`);
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("userId");
    localStorage.removeItem("userRoles");
  }
});

document
    .getElementById("togglePassword")
    .addEventListener("click", function () {

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
