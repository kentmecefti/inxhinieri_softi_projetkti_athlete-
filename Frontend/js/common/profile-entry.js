
const API = window.env?.API || "http://localhost:8080/api";
const BASE = "/athlete_tracker_front";

function decodeJwt(token) {
  try {
    const payload = token.split(".")[1];
    return JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
  } catch {
    return null;
  }
}

function getHeaders() {
  const token = localStorage.getItem("token");
  const h = { "Content-Type": "application/json" };
  if (token) h.Authorization = `Bearer ${token}`;
  return h;
}

function redirectToPersonalPage(mode, id) {
  if (mode === "ATHLETE") {
    location.href = `${BASE}/html/personal-athlete.html?athleteId=${id}`;
  } else if (mode === "COACH") {
    location.href = `${BASE}/html/personal-coach.html?coachId=${id}`;
  }
}

document.addEventListener("DOMContentLoaded", async () => {
  const token = localStorage.getItem("token");
  const decoded = decodeJwt(token);

  if (!token || !decoded) {
    localStorage.clear();
    location.href = `${BASE}/login/login.html`;
    return;
  }

  const params = new URLSearchParams(location.search);
  const ATHLETE_ID = params.get("athleteId");
  const COACH_ID = params.get("coachId");
  const MODE = ATHLETE_ID ? "ATHLETE" : COACH_ID ? "COACH" : null;

  if (!MODE) {
    alert("❌ Missing athleteId or coachId in URL");
    location.href = BASE;
    return;
  }

  const athleteForm = document.getElementById("athleteForm");
  const coachForm = document.getElementById("coachForm");

  if (MODE === "ATHLETE") {
    coachForm?.remove();
    athleteForm.classList.remove("hidden");

    const athUsername = document.getElementById("ath-username");
    const athName = document.getElementById("ath-name");
    const athLastname = document.getElementById("ath-lastname");
    const athGender = document.getElementById("ath-gender");
    const athBirthDate = document.getElementById("ath-birthDate");
    const athAge = document.getElementById("ath-age");
    const athWeight = document.getElementById("ath-weight");
    const athHeight = document.getElementById("ath-height");
    const athCategory = document.getElementById("ath-category");
    const athPerformance = document.getElementById("ath-performance");
    const athClub = document.getElementById("ath-club");
    const athCountry = document.getElementById("ath-country");
    const athCity = document.getElementById("ath-city");
    const athMsg = document.getElementById("ath-msg");

    try {
      const res = await fetch(`${API}/athletes/${ATHLETE_ID}`, {
        headers: getHeaders(),
      });

      if (!res.ok) throw new Error();
      const a = await res.json();

      athUsername.value = decoded.sub || "";
      athName.value = a.name ?? "";
      athLastname.value = a.lastname ?? "";
      athGender.value = a.gender ?? "Male";
      athBirthDate.value = a.birthDate ?? "";
      athAge.value = a.age ?? "";
      athWeight.value = a.athWeight ?? "";
      athHeight.value = a.athHeight ?? "";
      athCategory.value = a.category ?? "";
      athPerformance.value = a.performance ?? "";
      athClub.value = a.club ?? "";
      athCountry.value = a.country ?? "";
      athCity.value = a.city ?? "";
    } catch {
      alert("❌ Failed to load athlete profile");
      return;
    }

    athleteForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const body = {
        name: athName.value.trim(),
        lastname: athLastname.value.trim(),
        gender: athGender.value,
        birthDate: athBirthDate.value || null,
        age: athAge.value ? Number(athAge.value) : null,
        athWeight: athWeight.value ? Number(athWeight.value) : null,
        athHeight: athHeight.value ? Number(athHeight.value) : null,
        category: athCategory.value.trim(),
        performance: athPerformance.value.trim(),
        club: athClub.value.trim(),
        country: athCountry.value.trim(),
        city: athCity.value.trim(),
      };

      try {
        const res = await fetch(`${API}/athletes/${ATHLETE_ID}`, {
          method: "PUT",
          headers: getHeaders(),
          body: JSON.stringify(body),
        });

        if (!res.ok) throw new Error();

        athMsg.textContent = "✅ Athlete updated successfully";

        setTimeout(() => {
          redirectToPersonalPage("ATHLETE", ATHLETE_ID);
        }, 800);
      } catch {
        athMsg.textContent = "❌ Update failed";
      }
    });

    return;
  }

  if (MODE === "COACH") {
    athleteForm?.remove();
    coachForm.classList.remove("hidden");

    const coachUsername = document.getElementById("coach-username");
    const coachName = document.getElementById("coach-name");
    const coachLastname = document.getElementById("coach-lastname");
    const coachGender = document.getElementById("coach-gender");
    const coachExperienceYears = document.getElementById("coach-experienceYears");
    const coachSpecialization = document.getElementById("coach-specialization");
    const coachPhone = document.getElementById("coach-phone");
    const coachClub = document.getElementById("coach-club");
    const coachCountry = document.getElementById("coach-country");
    const coachMsg = document.getElementById("coach-msg");

    try {
      const res = await fetch(`${API}/coaches/${COACH_ID}`, {
        headers: getHeaders(),
      });

      if (!res.ok) throw new Error();
      const c = await res.json();

      coachUsername.value = decoded.sub || "";
      coachName.value = c.name ?? "";
      coachLastname.value = c.lastname ?? "";
      coachGender.value = c.gender ?? "Male";
      coachExperienceYears.value = c.experienceYears ?? "";
      coachSpecialization.value = c.specialization ?? "";
      coachPhone.value = c.phone ?? "";
      coachClub.value = c.club ?? "";
      coachCountry.value = c.country ?? "";
    } catch {
      alert("❌ Failed to load coach profile");
      return;
    }

    coachForm.addEventListener("submit", async (e) => {
      e.preventDefault();

      const body = {
        name: coachName.value.trim(),
        lastname: coachLastname.value.trim(),
        gender: coachGender.value,
        experienceYears: coachExperienceYears.value
          ? Number(coachExperienceYears.value)
          : 0,
        specialization: coachSpecialization.value.trim(),
        phone: coachPhone.value.trim(),
        club: coachClub.value.trim(),
        country: coachCountry.value.trim(),
      };

      try {
        const res = await fetch(`${API}/coaches/${COACH_ID}`, {
          method: "PUT",
          headers: getHeaders(),
          body: JSON.stringify(body),
        });

        if (!res.ok) throw new Error();

        coachMsg.textContent = "✅ Coach updated successfully";

        setTimeout(() => {
          redirectToPersonalPage("COACH", COACH_ID);
        }, 800);
      } catch {
        coachMsg.textContent = "❌ Update failed";
      }
    });

    return;
  }
});
