
function decodeJWT(token) {
  try {
    return JSON.parse(atob(token.split(".")[1]));
  } catch {
    return null;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  const sidebar = document.getElementById("sidebar");
  const toggleBtn = document.querySelector(".menu-toggle");
  const logoutBtn = document.getElementById("logoutBtn");
  const userTitle = document.getElementById("userTitle");
  const userRoleText = document.getElementById("userRoleText");

  function isDesktop() {
    return window.matchMedia("(min-width: 992px)").matches;
  }

  toggleBtn?.addEventListener("click", () => {
    if (!isDesktop()) sidebar?.classList.toggle("open");
  });

  const token = localStorage.getItem("token");

  let username = "Guest";
  let role = "GUEST";

  if (token) {
    const decoded = decodeJWT(token);
    if (decoded?.sub) {
      username = decoded.sub;
    }

    const storedRoles = localStorage.getItem("userRoles");
    if (storedRoles) {
      try {
        const rolesArr = JSON.parse(storedRoles);
        if (Array.isArray(rolesArr) && rolesArr.length > 0) {
          role = rolesArr[0].toUpperCase();
        }
      } catch {}
    }
  }

  userTitle.textContent = username;
  userRoleText.textContent = role;

  logoutBtn?.addEventListener("click", () => {
    localStorage.clear();
    window.location.href = "../html/common/login.html";
  });

  document.addEventListener("click", e => {
    if (
        !isDesktop() &&
        !sidebar?.contains(e.target) &&
        !toggleBtn?.contains(e.target)
    ) {
      sidebar?.classList.remove("open");
    }
  });
});

function openFrame(url) {
  const frame = document.getElementById("contentFrame");
  const container = document.getElementById("iframeContainer");

  if (frame && container) {
    frame.src = url;
    container.classList.remove("hidden");
  }
}
