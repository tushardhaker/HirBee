console.log("✅ SESSION SCRIPT RUNNING");

document.addEventListener("DOMContentLoaded", async () => {

    const email = localStorage.getItem("userEmail");

    if (!email) {
        console.log("🚫 No stored session");
        return;
    }

    try {

        const res = await fetch(`https://hirbee-1.onrender.com/api/auth/user?email=${email}`);

        if (!res.ok) {
            console.warn("⚠ Session invalid — NOT clearing storage");
            return;   // ✅ FIXED (NO CLEAR)
        }

        const user = await res.json();

        console.log("✅ Session Restored:", user);

        // ✅ ALWAYS RE-SAVE DATA
        localStorage.setItem("userRole", user.role);
        localStorage.setItem("fullName", user.fullName || "");
        localStorage.setItem("profileImage", user.profileImage || "");
        localStorage.setItem("location", user.location || "");

        updateNavbarUI(user);

    } catch (err) {
        console.log("⚠ Session restore failed — network issue");
    }
});

function updateNavbarUI(user) {

    document.querySelectorAll("a, button").forEach(el => {

        if (!el.innerText) return;

        if (
            el.innerText.includes("Login") ||
            el.innerText.includes("Sign Up")
        ) {
            el.style.display = "none";
        }
    });

    if (document.getElementById("headerUserName")) {
        document.getElementById("headerUserName").innerText =
            user.fullName?.split(" ")[0] || "Account";
    }
}
