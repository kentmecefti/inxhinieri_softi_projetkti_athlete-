
function getUserRoles() {
    try {
        const roles = JSON.parse(localStorage.getItem("userRoles"));
        return Array.isArray(roles) ? roles : [];
    } catch {
        return [];
    }
}

function allowAccess(config) {
    const roles = getUserRoles();
    const currentURL = window.location.pathname.toLowerCase();

    console.log("🔍 User roles:", roles);
    console.log("📄 Current page:", currentURL);

    if (roles.length === 0) {
        console.warn("⚠️ No roles found — redirecting to login.");
        window.location.href = "/login/login.html";
        return;
    }

    let allowed = roles.some(role => {
        const cleanRole = role.replace("ROLE_", "");
        console.log("Checking role:", cleanRole);

        const list = config[cleanRole];
        if (!list) return false;

        const file = currentURL.split("/").pop();
        return list.some(page => file === page.toLowerCase());

    });

    if (!allowed) {
        console.error("⛔ ACCESS DENIED");
        document.body.innerHTML = `
          <style>
            body { margin:0; background:transparent; overflow:hidden; }
            .wrappers {
              width:100vw;
              height:100vh;
              display:flex;
              justify-content:center;
              align-items:center;
            }
            .wrappers img {
              width:65%;
              max-width:1200px;
            }
          </style>
          <div class="wrappers">
            <img src="/images/403.png">
          </div>
        `;
    }
}
allowAccess({
    "ADMIN": [
        "all-users.html",
        "personal-athlete.html",
        "personal-coach.html",
        "new-runner.html",
        "jumper.html",
        "throw.html",
        "gym.html",
        "marathon.html",
        "athlete.html"
    ],

    "COACH": [
        "personal-coach.html",
        "athlete.html",
        "new-runner.html",
        "jumper.html",
        "throw.html",
        "gym.html",
        "marathon.html",
        "plan.html",
        "coach-info.html"
    ],

    "ATHLETE": [
        "personal-athlete.html",
        "new-runner.html",
        "jumper.html",
        "throw.html",
        "gym.html",
        "marathon.html",
        "plan.html",
        "request.html",
        "athlete-info.html"
    ]
});
