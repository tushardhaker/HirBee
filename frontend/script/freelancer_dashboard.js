// 🔥 BULLETPROOF SUBSCRIPTION SYSTEM - Freelancer Side

let allJobsGlobal = [];
let recentUpdates = [];
let lastSeenCount = parseInt(localStorage.getItem('lastSeenNotifCount') || "0");

// 💯 MAIN INITIALIZATION
document.addEventListener('DOMContentLoaded', function () {
    console.log('🚀 Freelancer Dashboard Loaded');

    refreshSubscriptionStatus();
    refreshUI();

    const city = localStorage.getItem('location') || "";
    loadFreelancerFeed(city);

    checkStatusUpdates();
    setInterval(checkStatusUpdates, 15000);

    setupProfileModal();

    setInterval(async () => {
        if (document.hasFocus()) await refreshSubscriptionStatus();
    }, 180000);
});


// ================================================
// ✅ SUBSCRIPTION STATUS – ALWAYS FROM SERVER
// ================================================

async function refreshSubscriptionStatus() {
    const email = localStorage.getItem('userEmail');
    if (!email) {
        updateVerifiedUI(false);
        return;
    }

    try {
        const res = await fetch(`https://hirbee-1.onrender.com/api/auth/user?email=${email}&_=${Date.now()}`);

        if (!res.ok) {
            updateVerifiedUI(false);
            return;
        }

        const user = await res.json();

        const plan = user.subscription_plan || 'FREE';
        const status = user.subscription_status || 'NONE';
        const endDateStr = user.subscription_end_date;
        const isTrending = user.is_trending || false;

        let isSubscribed = false;

        if (plan !== 'FREE' && status === 'ACTIVE') {
            if (!endDateStr) {
                isSubscribed = true;
            } else {
                const expiry = new Date(endDateStr);
                isSubscribed = expiry > new Date();
            }
        }

        updateVerifiedUI(isSubscribed && isTrending, plan);

    } catch (err) {
        console.error("Subscription refresh failed:", err);
        updateVerifiedUI(false);
    }
}

function updateVerifiedUI(isSubscribed, planName = 'Free') {

    ['headerVerifiedBadge', 'sidebarVerifiedBadge'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.toggle('d-none', !isSubscribed);
    });

    const subNav = document.getElementById('subscriptionNavItem');
    if (!subNav) return;

    subNav.innerHTML = isSubscribed
        ? `<button class="btn btn-sm w-100 rounded-pill py-2 mb-2 fw-bold" disabled>
             Premium Member (${planName})
           </button>`
        : `<button class="btn btn-sm w-100 rounded-pill py-2 mb-2 fw-bold"
             data-bs-toggle="modal" data-bs-target="#subscriptionModal">
             Upgrade Plan
           </button>`;
}


// ================================================
// ✅ PROFILE UI REFRESH
// ================================================

function refreshUI() {
    const name = localStorage.getItem('fullName') || "Freelancer";
    const loc = localStorage.getItem('location') || "Not Set";
    const img = localStorage.getItem('profileImage');

    document.getElementById('freelancerName')?.textContent = name;
    document.getElementById('headerUserName')?.textContent = name.split(' ')[0];
    document.getElementById('displayLocation')?.innerHTML = `<i class="bi bi-geo-alt-fill"></i> ${loc}`;

    if (img && img !== "null") {
        document.getElementById('sidebarAvatar') &&
            (document.getElementById('sidebarAvatar').innerHTML = `<img src="${img}" class="sidebar-avatar-img">`);

        document.getElementById('navProfileIcon') &&
            (document.getElementById('navProfileIcon').innerHTML = `<img src="${img}" class="nav-profile-img">`);
    }
}


// ================================================
// ✅ PROFILE MODAL
// ================================================

function setupProfileModal() {
    const modal = document.getElementById('updateProfileModal');

    modal?.addEventListener('show.bs.modal', () => {
        document.getElementById('updateName').value = localStorage.getItem('fullName') || "";
        document.getElementById('updateMobile').value = localStorage.getItem('mobile') || "";
        document.getElementById('updateLocation').value = localStorage.getItem('location') || "";
    });
}


// ================================================
// ✅ BULLETPROOF PROFILE SAVE (FIXED)
// ================================================

document.getElementById('profileUpdateForm')?.addEventListener('submit', async (e) => {

    e.preventDefault();

    const saveBtn = document.getElementById('profileSaveBtn');

    saveBtn && (saveBtn.disabled = true);
    saveBtn && (saveBtn.innerHTML = "Saving...");

    const payload = {
        email: localStorage.getItem('userEmail'),
        fullName: document.getElementById('updateName')?.value,
        mobile: document.getElementById('updateMobile')?.value,
        location: document.getElementById('updateLocation')?.value,
        profileImage: localStorage.getItem('profileImage')
    };

    try {

        const response = await fetch('https://hirbee-1.onrender.com/api/auth/update-profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error("Update failed");

        const data = await response.json();

        localStorage.setItem('fullName', data.fullName || "");
        localStorage.setItem('mobile', data.mobile || "");
        localStorage.setItem('location', data.location || "");
        localStorage.setItem('profileImage', data.profileImage || "");

        document.getElementById('profileSuccessOverlay')?.style.display = 'flex';

        setTimeout(() => location.reload(), 1500);

    } catch (err) {

        console.error(err);

        alert("Profile update failed");

        saveBtn && (saveBtn.disabled = false);
        saveBtn && (saveBtn.innerHTML = "Save Changes");
    }
});


// ================================================
// ✅ JOB FEED
// ================================================

async function loadFreelancerFeed(city) {

    const container = document.getElementById('feedContainer');
    const myLocation = city || localStorage.getItem('location') || "GLOBAL";

    try {
        const response = await fetch(`https://hirbee-1.onrender.com/api/jobs/freelancer-feed?city=${encodeURIComponent(myLocation)}`);

        allJobsGlobal = await response.json();

        renderJobs(allJobsGlobal);

    } catch {
        container.innerHTML = `<div class="text-danger text-center p-5">Server Error</div>`;
    }
}

function renderJobs(jobs) {

    const container = document.getElementById('feedContainer');

    if (!Array.isArray(jobs) || jobs.length === 0) {
        container.innerHTML = `<div class="text-muted text-center p-5">No Jobs Found</div>`;
        return;
    }

    container.innerHTML = jobs.map(job => `
        <div class="col-md-4 mb-3">
            <div class="card shadow-sm rounded-4 p-3">
                <h6 class="fw-bold">${job.details}</h6>
                <div class="d-flex justify-content-between">
                    <span>₹${job.budget}</span>
                    <button class="btn btn-sm btn-primary rounded-pill"
                        onclick="prepareApplyModal(${job.id})">
                        Apply
                    </button>
                </div>
            </div>
        </div>`).join('');
}


// ================================================
// ✅ LOGOUT
// ================================================

function logout() {
    localStorage.clear();
    sessionStorage.clear();
    window.location.href = "login.html";
}

console.log('✅ CLEAN DASHBOARD JS LOADED');
