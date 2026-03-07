/**
 * Global Instance
 */
const forgotModal = new bootstrap.Modal(document.getElementById('forgotModal'));

/**
 * Update Loading Text Dynamically
 */
function startLoadingMessages() {
    const textElement = document.getElementById('loadingText');
    const messages = [
        "Preparing your personalized workspace...",
        "Setting up your secure profile...",
        "Fetching your recent activities...",
        "Finalizing your dashboard..."
    ];
    let i = 0;
    const interval = setInterval(() => {
        i++;
        if (i < messages.length) {
            textElement.innerText = messages[i];
        } else {
            clearInterval(interval);
        }
    }, 800);
}

/**
 * Status Message Helpers
 */
function showStatus(message, type) {
    const box = document.getElementById('authMessage');
    box.innerText = message;
    box.classList.remove('d-none');
    box.style.backgroundColor = type === 'success' ? '#d1e7dd' : '#f8d7da';
    box.style.color = type === 'success' ? '#0f5132' : '#842029';
    box.style.border = type === 'success' ? '1px solid #badbcc' : '1px solid #f5c2c7';
}

function showModalStatus(message, type) {
    const msgDiv = document.getElementById('modalMessage');
    msgDiv.innerText = message;
    msgDiv.classList.remove('d-none');
    msgDiv.style.backgroundColor = type === 'success' ? '#d1e7dd' : '#f8d7da';
    msgDiv.style.color = type === 'success' ? '#0f5132' : '#842029';
}

/**
 * Login logic
 */
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('loginBtn');
    document.getElementById('authMessage').classList.add('d-none');

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Verifying...';

    const payload = {
        email: document.getElementById('loginEmail').value,
        password: document.getElementById('loginPassword').value
    };

    try {
        const response = await fetch('https://hirbee-1.onrender.com/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        
        if (response.ok) {
            // SUCCESS UI
            document.getElementById('successOverlay').style.display = 'flex';
            startLoadingMessages();

            // DATA PERSISTENCE - Added location here
            localStorage.setItem('fullName', data.fullName);
            localStorage.setItem('userRole', data.role);
            localStorage.setItem('userEmail', data.email);
            localStorage.setItem('mobile', data.mobile || "");
            localStorage.setItem('location', data.location || "Not Set"); // YE LINE ADD KI HAI
            localStorage.setItem('profileImage', data.profileImage || "");

            setTimeout(() => {
                const role = data.role;
                if (role === 'ADMIN') window.location.href = "admin-dashboard.html";
                else if (role === 'CLIENT') window.location.href = "client-dashboard.html";
                else if (role === 'FREELANCER') window.location.href = "freelancer-dashboard.html";
            }, 3000);

        } else {
            showStatus(data.error || "Access Denied: Invalid Credentials", "error");
            btn.disabled = false;
            btn.innerHTML = "Continue to Dashboard";
        }
    } catch (err) {
        showStatus("Network Error: Could not connect to server.", "error");
        btn.disabled = false;
        btn.innerHTML = "Continue to Dashboard";
    }
});

/**
 * Forgot Password Flow
 */
async function sendResetOTP() {
    const email = document.getElementById('resetEmail').value;
    const btn = document.getElementById('sendResetBtn');

    if(!email) { showModalStatus("Identity (Email) is required.", "error"); return; }
    
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Verifying...';

    try {
        const response = await fetch('https://hirbee-1.onrender.com/api/auth/send-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, purpose: 'reset' })
        });

        if (response.ok) {
            showModalStatus("OTP sent! Please check your mailbox.", "success");
            document.getElementById('forgotStep1').style.display = 'none';
            document.getElementById('forgotStep2').style.display = 'block';
        } else {
            const errorData = await response.json();
            showModalStatus(errorData.error || "Account not found.", "error");
            btn.disabled = false;
            btn.innerHTML = "Verify Identity";
        }
    } catch (err) {
        showModalStatus("Failed to reach server.", "error");
        btn.disabled = false;
        btn.innerHTML = "Verify Identity";
    }
}

async function verifyAndReset() {
    const btn = document.getElementById('updatePassBtn');
    const payload = {
        email: document.getElementById('resetEmail').value,
        otp: document.getElementById('resetOTP').value,
        newPassword: document.getElementById('newPassword').value
    };

    if(!payload.otp || !payload.newPassword) {
        showModalStatus("Please complete all verification fields.", "error");
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Syncing...';

    try {
        const response = await fetch('https://hirbee-1.onrender.com/api/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            showModalStatus("Security updated! Finalizing...", "success");
            setTimeout(() => {
                forgotModal.hide();
                document.getElementById('forgotStep1').style.display = 'block';
                document.getElementById('forgotStep2').style.display = 'none';
                document.getElementById('modalMessage').classList.add('d-none');
            }, 2000);
        } else {
            const errorData = await response.json();
            showModalStatus(errorData.error || "Verification code mismatch.", "error");
            btn.disabled = false;
            btn.innerHTML = "Update Password";
        }
    } catch (err) {
        showModalStatus("Sync failed. Check connection.", "error");
        btn.disabled = false;
        btn.innerHTML = "Update Password";
    }
}

function loginWithGoogle() { window.location.href = "https://hirbee-1.onrender.com/oauth2/authorization/google"; }
function loginWithGitHub() { window.location.href = "https://hirbee-1.onrender.com/oauth2/authorization/github"; }
function openForgotModal() { forgotModal.show(); }