
// ================================================
// SAFE MODAL PRE-FILL – FIXED NULL ERROR HERE
// ================================================

document.addEventListener('DOMContentLoaded', () => {
    const modalEl = document.getElementById('updateProfileModal');
    
    if (!modalEl) {
        console.warn("Modal #updateProfileModal not found");
        return;
    }

    modalEl.addEventListener('show.bs.modal', () => {
        console.log("Modal opened – attempting safe pre-fill");

        // Safe pre-fill – no crash even if elements don't exist
        ['updateName', 'updateMobile', 'updateLocation'].forEach(id => {
            const field = document.getElementById(id);
            const value = localStorage.getItem(id.replace('update', '').toLowerCase()) || '';
            if (field) {
                field.value = value;
                console.log(`Filled #${id} with: ${value}`);
            } else {
                console.warn(`Field #${id} not found – skipping`);
            }
        });

        const preview = document.getElementById('profilePreview');
        const defIcon = document.getElementById('modalDefaultIcon');
        
        if (preview) {
            const img = localStorage.getItem('profileImage');
            if (img && img !== "null" && img !== "") {
                preview.src = img;
                preview.style.display = 'block';
                if (defIcon) defIcon.style.display = 'none';
                console.log("Profile preview loaded from localStorage");
            } else {
                preview.style.display = 'none';
                if (defIcon) defIcon.style.display = 'flex';
            }
        } else {
            console.warn("Preview #profilePreview not found – skipping image load");
        }
    });
});

// ================================================
// HELPER: READ MORE/LESS TOGGLE
// ================================================
function toggleDesc(id) {
    const textBase = document.getElementById(`desc-base-${id}`);
    const textMore = document.getElementById(`desc-more-${id}`);
    const btn = document.getElementById(`btn-toggle-${id}`);

    if (textMore.classList.contains('d-none')) {
        textMore.classList.remove('d-none');
        btn.innerText = "Read Less";
    } else {
        textMore.classList.add('d-none');
        btn.innerText = "Read More...";
    }
}

// ================================================
// YOUR ORIGINAL CODE – MODIFIED FOR READ MORE
// ================================================

document.addEventListener('DOMContentLoaded', () => {
    // 🔥 FAILSAFE: Restore subscription FIRST
    const subscriptionPlan = localStorage.getItem('subscriptionPlan');
    if (subscriptionPlan && !['null', 'undefined'].includes(subscriptionPlan)) {
        console.log('🔒 Subscription detected:', subscriptionPlan);
    }
    
    refreshUI();
    showSection('dashboard'); 
    updateStats();
    updateWalletBalance(); 

    checkClientProposals();
    setInterval(checkClientProposals, 10000); 

    // ✅ Check subscription status on load
    checkSubscriptionStatus();
});

// ✅ FIXED LOGOUT - Preserves subscriptionPlan FOREVER
function logout() {
    console.log('🔒 LOGOUT: Saving subscriptionPlan:', localStorage.getItem('subscriptionPlan'));
    const subscriptionBackup = localStorage.getItem('subscriptionPlan');
    
    localStorage.removeItem('userEmail');
    localStorage.removeItem('fullName');
    localStorage.removeItem('mobile');
    localStorage.removeItem('location');
    localStorage.removeItem('profileImage');
    localStorage.removeItem('lastSeenPropCount');
    
    if (subscriptionBackup) {
        localStorage.setItem('subscriptionPlan', subscriptionBackup);
    }
    
    window.location.href = 'login.html';
}

function checkSubscriptionStatus() {
    const subscriptionPlan = localStorage.getItem('subscriptionPlan');
    if (subscriptionPlan && ['STARTER', 'PREMIUM', 'BUSINESS'].includes(subscriptionPlan)) {
        updateVerifiedStatus(true);
    } else {
        updateVerifiedStatus(false);
    }
}

function updateVerifiedStatus(isVerified) {
    const badges = ['headerVerifiedBadge', 'sidebarVerifiedBadge', 'mobileVerifiedBadge'];
    badges.forEach(badgeId => {
        const badge = document.getElementById(badgeId);
        if (badge) {
            isVerified ? badge.classList.remove('d-none') : badge.classList.add('d-none');
        }
    });

    const planBadge = document.getElementById('userPlanBadge');
    if (planBadge) {
        if (isVerified) {
            const plan = localStorage.getItem('subscriptionPlan') || 'Premium';
            planBadge.innerText = `${plan} Plan`;
            planBadge.className = 'text-primary fw-bold';
        } else {
            planBadge.innerText = 'Free Plan';
            planBadge.className = 'text-muted';
        }
    }
}

let allReceivedProposals = [];
let lastSeenPropCount = parseInt(localStorage.getItem('lastSeenPropCount')) || 0;

function previewImage(event) {
    const file = event.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('profilePreview');
            const defIcon = document.getElementById('modalDefaultIcon');
            preview.src = e.target.result;
            preview.style.display = 'block';
            defIcon.style.display = 'none';
        }
        reader.readAsDataURL(file);
    }
}

const toBase64 = file => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
});

function refreshUI() {
    const name = localStorage.getItem('fullName') || "Client";
    const img = localStorage.getItem('profileImage');
    const loc = localStorage.getItem('location') || "Not Set";

    document.getElementById('clientName').innerText = name;
    document.getElementById('headerClientName').innerText = name.split(' ')[0];
    document.getElementById('welcomeName').innerText = name;
    document.getElementById('displayLocation').innerHTML = `<i class="bi bi-geo-alt-fill text-primary"></i> ${loc}`;
    
    if(document.getElementById('mobileClientName')) document.getElementById('mobileClientName').innerText = name;

    if(img && img !== "null" && img !== "") {
        const sidebar = document.getElementById('sidebarAvatar');
        const navIcon = document.getElementById('navProfileIcon');
        const mobileIcon = document.getElementById('mobileAvatar');
        
        const avatarImg = `<img src="${img}" class="profile-pic-container">`;
        if(sidebar) sidebar.innerHTML = avatarImg;
        if(navIcon) navIcon.innerHTML = avatarImg;
        if(mobileIcon) mobileIcon.innerHTML = avatarImg;
    }
    setTimeout(checkSubscriptionStatus, 50);
}

async function updateWalletBalance() {
    const email = localStorage.getItem('userEmail');
    if(!email) return;
    try {
        const res = await fetch(`https://hirbee-1.onrender.com/api/wallet/balance?email=${email}`);
        if (res.ok) {
            const data = await res.json();
            document.getElementById('walletBalance').innerText = data.balance.toFixed(2);
        }
    } catch (err) { console.error("Wallet Error:", err); }
}

async function updateStats() {
    const email = localStorage.getItem('userEmail');
    const clientLocation = localStorage.getItem('location');
    
    try {
        const jobRes = await fetch(`https://hirbee-1.onrender.com/api/jobs/my-jobs?email=${email}`);
        const jobs = await jobRes.json();
        document.getElementById('activeJobsCount').innerText = jobs.length;

        const propRes = await fetch(`https://hirbee-1.onrender.com/api/proposals/client-list?email=${email}`);
        const props = await propRes.json();
        document.getElementById('totalProposalsCount').innerText = props.length;

        if (clientLocation && clientLocation !== "Not Set" && clientLocation !== "Global") {
            const searchCity = clientLocation.split(' ')[0].replace(',', '');
            const freeRes = await fetch(`https://hirbee-1.onrender.com/api/auth/freelancers-by-location?location=${encodeURIComponent(searchCity)}`);
            if (freeRes.ok) {
                const nearby = await freeRes.json();
                document.getElementById('nearbyCount').innerText = nearby.length;
            }
        }
    } catch (err) { console.error("Stats Error:", err); }
}

async function checkClientProposals() {
    const email = localStorage.getItem('userEmail');
    if(!email) return;
    const headerBadge = document.getElementById('headerNotifBadge');

    try {
        const res = await fetch(`https://hirbee-1.onrender.com/api/proposals/client-list?email=${email}`);
        const proposals = await res.json();
        allReceivedProposals = proposals;
        
        const unseen = proposals.length - lastSeenPropCount;
        if (unseen > 0) {
            headerBadge.innerText = unseen;
            headerBadge.classList.remove('d-none');
        } else {
            headerBadge.classList.add('d-none');
        }
    } catch (err) { console.error("Notif Check Error:", err); }
}

function showClientNotifications() {
    const modalElement = document.getElementById('clientNotifModal');
    const list = document.getElementById('clientNotifList');
    const headerBadge = document.getElementById('headerNotifBadge');

    if (!modalElement || !list) return;

    lastSeenPropCount = allReceivedProposals.length;
    localStorage.setItem('lastSeenPropCount', lastSeenPropCount);
    if (headerBadge) headerBadge.classList.add('d-none');

    if (allReceivedProposals.length === 0) {
        list.innerHTML = '<div class="p-4 text-center text-muted">No proposals received yet.</div>';
    } else {
        list.innerHTML = [...allReceivedProposals].reverse().map(p => `
            <div class="list-group-item p-3 border-0 border-bottom">
                <div class="d-flex align-items-center">
                    <div class="bg-lavender p-2 rounded-circle text-primary me-3">
                        <i class="bi bi-file-earmark-person"></i>
                    </div>
                    <div>
                        <p class="mb-0 small"><b>${p.freelancerEmail}</b> applied for your project.</p>
                        <small class="text-primary fw-bold" style="cursor:pointer" onclick="showSection('proposals'); bootstrap.Modal.getInstance(document.getElementById('clientNotifModal')).hide();">
                            View Proposal
                        </small>
                    </div>
                </div>
            </div>
        `).join('');
    }
    new bootstrap.Modal(modalElement).show();
}

async function showSection(type) {
    const container = document.getElementById('dynamicContainer');
    const sectionTitle = document.getElementById('sectionTitle');
    const statsRow = document.getElementById('statsRow');
    const welcomeHeader = document.getElementById('welcomeHeader');
    const email = localStorage.getItem('userEmail');
    
    document.querySelectorAll('.sidebar .nav-link').forEach(l => l.classList.remove('active'));
    if(type==='dashboard' && document.getElementById('nav-dash')) document.getElementById('nav-dash').classList.add('active');
    if(type==='profile' && document.getElementById('nav-prof')) document.getElementById('nav-prof').classList.add('active');
    if(type==='projects' && document.getElementById('nav-proj')) document.getElementById('nav-proj').classList.add('active');
    if(type==='proposals' && document.getElementById('nav-prop')) document.getElementById('nav-prop').classList.add('active');

    container.innerHTML = '<div class="text-center p-5"><div class="spinner-border text-primary"></div></div>';
    statsRow.style.display = (type === 'dashboard') ? "flex" : "none";
    welcomeHeader.style.display = (type === 'dashboard' || type === 'profile') ? "flex" : "none";

    try {
        if (type === 'dashboard' || type === 'projects') {
            sectionTitle.innerText = (type === 'dashboard') ? "Recent Job Postings" : "All My Projects";
            const res = await fetch(`https://hirbee-1.onrender.com/api/jobs/my-jobs?email=${email}`);
            let jobs = await res.json();
            
            if (type === 'dashboard') jobs = jobs.sort((a, b) => b.id - a.id).slice(0, 3);
            
            if (jobs.length === 0) { 
                container.innerHTML = '<div class="col-12 text-center p-5"><h5>No jobs found.</h5><p>Start by posting a new project!</p></div>'; 
                return; 
            }

            container.innerHTML = jobs.map(job => {
                // READ MORE LOGIC
                const desc = job.details || "";
                const isLong = desc.length > 80;
                const displayDesc = isLong ? 
                    `<span id="desc-base-${job.id}">${desc.substring(0, 80)}</span><span id="desc-more-${job.id}" class="d-none">${desc.substring(80)}</span>` : 
                    desc;

                return `
                <div class="col-md-6 col-lg-4 mb-3">
                    <div class="card border-0 shadow-sm rounded-4 p-3 h-100">
                        <div class="d-flex justify-content-between mb-2">
                            <h6 class="fw-bold mb-1">Project Title</h6>
                            <span class="badge bg-primary-subtle text-primary rounded-pill">₹${job.budget}</span>
                        </div>
                        <p class="text-muted small mb-2" style="line-height:1.4">
                            ${displayDesc}
                            ${isLong ? `<br><button id="btn-toggle-${job.id}" onclick="toggleDesc(${job.id})" class="btn btn-link p-0 small fw-bold text-decoration-none" style="font-size: 0.75rem;">Read More...</button>` : ''}
                        </p>
                        <p class="text-muted small mb-3"><i class="bi bi-geo-alt"></i> ${job.location}</p>
                        <button class="btn btn-sm btn-outline-primary rounded-pill w-100" onclick="showSection('proposals')">View Bids</button>
                    </div>
                </div>`;
            }).join('');

        } else if (type === 'proposals') {
            sectionTitle.innerText = "Proposals Received";
            const res = await fetch(`https://hirbee-1.onrender.com/api/proposals/client-list?email=${email}`);
            const props = await res.json();
            if (props.length === 0) { container.innerHTML = '<div class="col-12 text-center p-5">No proposals yet.</div>'; return; }
            
            container.innerHTML = props.map(p => {
                const cover = p.coverLetter || "";
                const isLong = cover.length > 100;
                const displayCover = isLong ? 
                    `<span id="desc-base-prop-${p.id}">${cover.substring(0, 100)}</span><span id="desc-more-prop-${p.id}" class="d-none">${cover.substring(100)}</span>` : 
                    cover;

                return `
                <div class="col-md-6 mb-3">
                    <div class="card border-0 shadow-sm rounded-4 p-4 border-start border-success border-4 h-100">
                        <h6 class="fw-bold text-dark mb-0">${p.freelancerEmail}</h6>
                        <span class="badge bg-success mb-2 w-auto" style="display:inline-block">₹${p.bidAmount}</span>
                        <div class="bg-light p-2 rounded small mb-3">
                            ${displayCover}
                            ${isLong ? `<br><button id="btn-toggle-prop-${p.id}" onclick="toggleDesc('prop-${p.id}')" class="btn btn-link p-0 small fw-bold text-decoration-none" style="font-size: 0.75rem;">Read More...</button>` : ''}
                        </div>
                        <button class="btn btn-primary btn-sm w-100 rounded-pill" onclick="updateProposalStatus(${p.id}, 'ACCEPTED')">Hire</button>
                    </div>
                </div>`;
            }).join('');

        } else if (type === 'profile') {
            sectionTitle.innerText = "My Account Profile";
            const name = localStorage.getItem('fullName') || "Not Set";
            const mobile = localStorage.getItem('mobile') || "Not Set";
            const loc = localStorage.getItem('location') || "Not Set";
            const balance = document.getElementById('walletBalance')?.innerText || '0.00';
            const img = localStorage.getItem('profileImage');
            
            const avatarHtml = (img && img !== "null") 
                ? `<img src="${img}" class="rounded-circle shadow" style="width: 120px; height: 120px; object-fit: cover; border: 4px solid white;">`
                : `<div class="bg-primary text-white rounded-circle d-flex align-items-center justify-content-center shadow" style="width: 120px; height: 120px; font-size: 3.5rem;"><i class="bi bi-person-fill"></i></div>`;

            container.innerHTML = `
                <div class="col-12">
                    <div class="card border-0 shadow-sm rounded-4 p-4">
                    <div class="row align-items-center">
                        <div class="col-md-auto text-center mb-3 mb-md-0">${avatarHtml}</div>
                        <div class="col-md ms-md-4">
                            <h3 class="fw-bold mb-1">${name}</h3>
                            <p class="text-muted mb-4"><i class="bi bi-envelope-at me-2"></i>${email || 'Not logged in'}</p>
                            <div class="row g-3">
                                <div class="col-sm-6 col-lg-4"><div class="p-3 border rounded-4 bg-white shadow-sm"><small class="text-muted d-block fw-bold mb-1">MOBILE</small><div class="fw-semibold text-dark">${mobile}</div></div></div>
                                <div class="col-sm-6 col-lg-4"><div class="p-3 border rounded-4 bg-white shadow-sm"><small class="text-muted d-block fw-bold mb-1">CITY</small><div class="fw-semibold text-dark">${loc}</div></div></div>
                                <div class="col-sm-6 col-lg-4"><div class="p-3 border rounded-4 bg-white shadow-sm"><small class="text-muted d-block fw-bold mb-1">WALLET</small><div class="fw-semibold text-success">₹${balance}</div></div></div>
                            </div>
                            <button class="btn btn-brand-primary rounded-pill mt-4 px-4 py-2" data-bs-toggle="modal" data-bs-target="#updateProfileModal"><i class="bi bi-pencil-square me-2"></i>Edit Details</button>
                        </div>
                    </div>
                    </div>
                </div>`;
        }
    } catch (e) { 
        container.innerHTML = `<p class="text-center text-danger">Failed to load content.</p>`; 
    }
}

// ... Rest of the functions (fetchNearbyFreelancers, fetchGeoLocation, updateProposalStatus, etc.) same as before ...

async function fetchNearbyFreelancers() {
    const container = document.getElementById('dynamicContainer');
    const sectionTitle = document.getElementById('sectionTitle');
    const statsRow = document.getElementById('statsRow');
    const clientLocation = localStorage.getItem('location');

    if (!clientLocation || clientLocation === "Not Set") {
        alert("Please set location in profile first!"); return;
    }

    statsRow.style.display = "none";
    sectionTitle.innerText = `Freelancers in ${clientLocation}`;
    const searchCity = clientLocation.split(' ')[0].replace(',', '');
    const res = await fetch(`https://hirbee-1.onrender.com/api/auth/freelancers-by-location?location=${encodeURIComponent(searchCity)}`);
    const freelancers = await res.json();

    if(freelancers.length === 0) {
        container.innerHTML = '<p class="text-center p-5">No local freelancers found.</p>';
    } else {
        container.innerHTML = freelancers.map(f => `
            <div class="col-md-4 mb-3">
            <div class="card border-0 shadow-sm rounded-4 p-3 h-100 text-center">
                <div class="bg-primary-subtle text-primary rounded-circle mx-auto d-flex align-items-center justify-content-center mb-3" style="width: 50px; height: 50px;">
                <i class="bi bi-person-fill"></i>
                </div>
                <h6 class="fw-bold mb-1">${f.fullName}</h6>
                <p class="text-muted small mb-3">${f.skills || 'Expert'}</p>
                <button class="btn btn-sm btn-primary rounded-pill w-100" onclick="window.location.href='message.html?with=${f.email}'">Message</button>
            </div>
            </div>`).join('');
    }
}

async function fetchGeoLocation() {
    const input = document.getElementById('updateLocation');
    if (navigator.geolocation) {
        input.value = "Detecting...";
        navigator.geolocation.getCurrentPosition(async (pos) => {
            const { latitude, longitude } = pos.coords;
            const res = await fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}`);
            const data = await res.json();
            input.value = data.address.city || data.address.town || "Global";
        });
    }
}

document.getElementById('profileUpdateForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitBtn = document.getElementById('profileSaveBtn');
    submitBtn.innerText = "Updating...";
    submitBtn.disabled = true;

    const payload = {
        email: localStorage.getItem('userEmail'),
        fullName: document.getElementById('updateName').value,
        mobile: document.getElementById('updateMobile').value,
        location: document.getElementById('updateLocation').value,
        profileImage: localStorage.getItem('profileImage') 
    };

    const photoFile = document.getElementById('updatePhoto').files[0];
    if (photoFile) {
        payload.profileImage = await toBase64(photoFile);
    }

    try {
        const res = await fetch('https://hirbee-1.onrender.com/api/auth/update-profile', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) { 
            const data = await res.json();
            localStorage.setItem('fullName', data.fullName);
            localStorage.setItem('mobile', data.mobile);
            localStorage.setItem('location', data.location);
            localStorage.setItem('profileImage', data.profileImage);
            location.reload();
        } else {
            alert("Update failed!");
            submitBtn.disabled = false;
            submitBtn.innerText = "Save Changes";
        }
    } catch (err) {
        alert("Server error!");
        submitBtn.disabled = false;
        submitBtn.innerText = "Save Changes";
    }
});

async function updateProposalStatus(proposalId, status) {
    if (status === 'ACCEPTED') {
        const propRes = await fetch(`https://hirbee-1.onrender.com/api/proposals/client-list?email=${localStorage.getItem('userEmail')}`);
        const props = await propRes.json();
        const p = props.find(x => x.id === proposalId);

        if (confirm(`Hiring will deduct ₹${p.bidAmount} from your wallet. Proceed?`)) {
            const payRes = await fetch('https://hirbee-1.onrender.com/api/wallet/transfer', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    senderEmail: localStorage.getItem('userEmail'),
                    receiverEmail: p.freelancerEmail,
                    amount: p.bidAmount
                })
            });
            if (!payRes.ok) { alert(await payRes.text()); return; }
        } else { return; }
    }
    const res = await fetch(`https://hirbee-1.onrender.com/api/proposals/update-status?id=${proposalId}&status=${status}`, { method: 'POST' });
    if (res.ok) { alert("Action successful!"); location.reload(); }
}

console.log('✅ Dashboard JS Fully Loaded – With Read More Logic');