// Base API URLs
const API_URL = "https://hirbee-1.onrender.com/api/admin";
const AUTH_API_URL = "https://hirbee-1.onrender.com/api/auth"; // Auth Controller for subscriptions
const BANK_API_URL = "https://hirbee-1.onrender.com/api/bank";

/**
 * 1. Tab Switching System
 */
function switchTab(tabName) {
    // Hide all sections
    document.getElementById('overview-tab').classList.add('d-none');
    document.getElementById('finance-tab').classList.add('d-none');
    
    // Remove active class from all links
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });

    // Show target section
    const targetSection = document.getElementById(tabName + '-tab');
    if (targetSection) {
        targetSection.classList.remove('d-none');
        const activeNav = document.querySelector(`[onclick="switchTab('${tabName}')"]`);
        if(activeNav) activeNav.classList.add('active');
    }

    // Refresh data based on tab
    if (tabName === 'overview') {
        loadDashboardStats();
        fetchTrendingRequests(); 
    }
    if (tabName === 'finance') {
        loadDashboardStats();
        loadAdminBank(); 
    }
}

/**
 * 2. Load Dashboard Stats (Overview)
 */
async function loadDashboardStats() {
    try {
        const response = await fetch(`${API_URL}/stats`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        
        const stats = await response.json();
        
        updateElementText('stat-total-users', stats.totalUsers || 0);
        updateElementText('stat-jobs', stats.totalJobs || 0);
        updateElementText('stat-locations', stats.totalLocations || 0);

        const balance = (stats.adminWalletBalance || 0).toLocaleString('en-IN');
        updateElementText('stat-admin-balance', `₹${balance}`);
        
        const revenueElement = document.getElementById('platform-revenue');
        if(revenueElement) {
            revenueElement.innerText = `₹${(stats.adminWalletBalance || 0).toLocaleString('en-IN', {minimumFractionDigits: 2})}`;
        }

    } catch (error) {
        console.error("Dashboard Stats Error:", error);
    }
}

/**
 * 3. Fetch ACTIVE Trending Members (Updated for Active Status + Hide STARTER)
 */
async function fetchTrendingRequests() {
    const tbody = document.getElementById('trendingRequestsBody');
    if (!tbody) return;

    try {
        // Calling active subscriptions endpoint
        const response = await fetch(`${AUTH_API_URL}/admin/subscriptions`);
        const users = await response.json();
        
        // Filter out STARTER / BASIC / FREE users
        const filteredUsers = users.filter(user => {
            const plan = (user.subscriptionPlan || '').toUpperCase();
            return plan !== 'STARTER' && plan !== 'BASIC' && plan !== 'FREE' && plan !== '';
        });

        if (filteredUsers.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" class="text-center py-5 text-muted">No active premium members found.</td></tr>`;
            return;
        }

        tbody.innerHTML = filteredUsers.map(user => `
            <tr>
                <td class="ps-4">
                    <div class="d-flex align-items-center">
                        <div class="bg-soft-purple rounded-circle me-3 d-flex align-items-center justify-content-center" style="width: 38px; height: 38px; background: #f3eafa; color: #8C4EC9;">
                            <i class="bi bi-person-badge"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark">${user.fullName || 'User'}</div>
                            <div class="small text-muted">${user.email}</div>
                        </div>
                    </div>
                </td>
                <td>
                    <span class="badge rounded-pill border text-dark fw-bold px-3 py-2" style="background: #f8f9fa;">
                        <i class="bi bi-gem text-primary me-1"></i> ${user.subscriptionPlan || 'PREMIUM'}
                    </span>
                </td>
                <td>
                    <span class="badge bg-success-subtle text-success px-3 py-2 rounded-pill">
                        <i class="bi bi-check-circle-fill me-1"></i> ACTIVE
                    </span>
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-danger rounded-pill px-3" 
                            onclick="removeSubscription('${user.email}')">
                        Revoke Access
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error("Trending Fetch Error:", err);
        tbody.innerHTML = `<tr><td colspan="4" class="text-center py-4 text-danger">Service Unavailable.</td></tr>`;
    }
}

/**
 * 4. Remove/Revoke Subscription Action
 */
async function removeSubscription(email) {
    if(!confirm(`Are you sure you want to remove premium status for ${email}?`)) return;

    try {
        const response = await fetch(`${AUTH_API_URL}/admin/remove-subscription?email=${email}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            alert("Success! User access revoked.");
            fetchTrendingRequests(); 
            loadDashboardStats();
        } else {
            alert("Failed to revoke access.");
        }
    } catch (err) {
        console.error("Revoke Error:", err);
    }
}

/**
 * 5. Load Admin Bank
 */
async function loadAdminBank() {
    const adminEmail = localStorage.getItem('userEmail') || "admin@gmail.com";
    const displayArea = document.getElementById('adminBankDisplay');
    const actionBtnArea = document.getElementById('bankActionBtn');

    if(!displayArea) return;

    try {
        const response = await fetch(`${BANK_API_URL}/my-details?email=${encodeURIComponent(adminEmail)}`);
        
        if (response.ok) {
            const bank = await response.json();
            if(actionBtnArea) actionBtnArea.innerHTML = `<a href="bank.html" class="btn btn-sm btn-outline-primary rounded-pill px-3 fw-bold"><i class="bi bi-pencil-square me-1"></i>Edit Bank</a>`;
            
            displayArea.className = "p-4 bg-white rounded-4 shadow-sm border-start border-primary border-5 text-start";
            displayArea.innerHTML = `
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="text-muted small fw-bold d-block">HOLDER NAME</label>
                        <span class="fw-800 text-dark-purple">${bank.accountHolderName}</span>
                    </div>
                    <div class="col-md-6 mb-3 text-md-end">
                        <span class="badge bg-soft-success text-success px-3 rounded-pill">Active Node</span>
                    </div>
                    <div class="col-md-6">
                        <label class="text-muted small fw-bold d-block">BANK / IFSC</label>
                        <span class="fw-bold">${bank.bankName}</span>
                    </div>
                    <div class="col-md-6 text-md-end">
                        <label class="text-muted small fw-bold d-block">ACCOUNT NO.</label>
                        <span class="fw-bold text-primary">****${bank.accountNumber.slice(-4)}</span>
                    </div>
                </div>`;
        } else {
            displayArea.innerHTML = `<div class="py-4"><h5 class="fw-bold mt-3">Vault Not Connected</h5><a href="bank.html" class="btn btn-primary rounded-pill px-5">Setup Vault</a></div>`;
        }
    } catch (error) {
        displayArea.innerHTML = `<p class="text-danger p-4">Bank Service connection timed out.</p>`;
    }
}

/**
 * Utility Functions
 */
function updateElementText(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}

function logout() {
    localStorage.clear();
    window.location.href = "login.html";
}

// System Init
document.addEventListener('DOMContentLoaded', () => {
    loadDashboardStats();
    fetchTrendingRequests(); 
});