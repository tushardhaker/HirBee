/**
 * Global variables
 */
const API_BASE_URL = 'https://hirbee-1.onrender.com/api/auth';

/**
 * Handle Tutorial / Tour Logic
 */
window.addEventListener('load', function() {
    if (!localStorage.getItem('signup_tour_done')) {
        startSignupTour();
    }
});

function startSignupTour() {
    introJs().setOptions({
        steps: [
            {
                title: 'Welcome! 👋',
                intro: 'Welcome to the Registration page. Let us help you set up your account in a few seconds.'
            },
            {
                element: '#nameStep',
                intro: 'Please enter your full legal name as it will appear on your profile.',
                position: 'bottom'
            },
            {
                element: '#emailStep',
                intro: 'Enter your email and click **OTP** to verify your account. This is required for security.',
                position: 'bottom'
            },
            {
                element: '#mobileStep',
                intro: 'Provide your mobile number so we can reach you for urgent updates.',
                position: 'bottom'
            },
            {
                element: '#passwordStep',
                intro: 'Create a strong password (at least 8 characters recommended).',
                position: 'bottom'
            },
            {
                element: '#roleStep',
                intro: 'Select **Freelancer** to find work, or **Client** to hire professionals.',
                position: 'top'
            },
            {
                element: '#socialStep',
                intro: 'Alternatively, you can skip the form and sign up instantly using Google or GitHub.',
                position: 'top'
            },
            {
                element: '#registerBtn',
                intro: 'Once everything is filled, click here to join our elite network!',
                position: 'top'
            }
        ],
        showProgress: true,
        exitOnOverlayClick: false,
        nextLabel: 'Next →',
        prevLabel: '← Back',
        skipLabel: 'Skip',
        doneLabel: 'Finish'
    }).oncomplete(function() {
        localStorage.setItem('signup_tour_done', 'true');
    }).onexit(function() {
        localStorage.setItem('signup_tour_done', 'true');
    }).start();
}

/**
 * Handle OTP Request
 */
async function sendOTP(type) {
    const email = document.getElementById('email').value;
    const otpBtn = document.getElementById('otpBtn');
    const statusDiv = document.getElementById('otpStatus');
    const otpGroup = document.getElementById('otpGroup');

    if (!email || !document.getElementById('email').checkValidity()) {
        showStatus("otpStatus", "Please enter a valid email.", "error");
        return;
    }

    // UI Loading
    otpBtn.disabled = true;
    otpBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
    showStatus("otpStatus", "Sending secure OTP...", "success");

    try {
        const response = await fetch(`${API_BASE_URL}/send-otp`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, purpose: 'registration' })
        });

        if (response.ok) {
            showStatus("otpStatus", "OTP sent! Check your inbox.", "success");
            otpBtn.innerHTML = '<i class="bi bi-check2"></i>';
            otpGroup.style.display = 'block';
        } else {
            const data = await response.json();
            showStatus("otpStatus", data.error || "Failed to send OTP", "error");
            otpBtn.innerHTML = "OTP";
            otpBtn.disabled = false;
        }
    } catch (err) {
        showStatus("otpStatus", "Server not responding.", "error");
        otpBtn.innerHTML = "OTP";
        otpBtn.disabled = false;
    }
}

/**
 * Final Registration
 */
document.getElementById('signupForm').addEventListener('submit', async function (e) {
    e.preventDefault();

    const registerBtn = document.getElementById('registerBtn');
    const otpInput = document.getElementById('otpInput').value;
    const termsChecked = document.getElementById('terms').checked;
    const formStatus = document.getElementById('formStatus');

    if (!termsChecked) {
        showStatus("formStatus", "Please accept terms & conditions.", "error");
        return;
    }

    if (!otpInput || otpInput.length < 6) {
        showStatus("formStatus", "Please enter the 6-digit OTP first.", "error");
        return;
    }

    // UI Loading State
    registerBtn.disabled = true;
    registerBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Creating Account...';

    const payload = {
        fullName: document.getElementById('fullName').value.trim(),
        email: document.getElementById('email').value.trim(),
        mobile: document.getElementById('mobile').value.trim(),
        password: document.getElementById('password').value,
        role: document.querySelector('input[name="role"]:checked').value,
        otp: otpInput,
        
        // Newsletter subscription (optional checkbox value)
        newsletterOptIn: document.getElementById('newsletterOptIn').checked ? "true" : "false"
    };

    try {
        const response = await fetch(`${API_BASE_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            // SUCCESS: Show Overlay instead of Alert
            document.getElementById('successOverlay').style.display = 'flex';
            
            // Redirect after 3 seconds
            setTimeout(() => {
                window.location.href = "login.html";
            }, 3000);
        } else {
            const errorData = await response.json();
            showStatus("formStatus", "Failed: " + (errorData.error || "Registration failed"), "error");
            registerBtn.disabled = false;
            registerBtn.innerHTML = "Register Now";
        }
    } catch (err) {
        showStatus("formStatus", "Network error. Connection failed.", "error");
        registerBtn.disabled = false;
        registerBtn.innerHTML = "Register Now";
    }
});

/** Helper for Status messages */
function showStatus(elementId, msg, type) {
    const div = document.getElementById(elementId);
    div.style.display = 'block';
    div.innerText = msg;
    div.className = `small mt-1 fw-bold ${type === 'success' ? 'status-success' : 'status-error'}`;
}

/** Google Login Redirect */
function loginWithGoogle() {
    window.location.href = "https://hirbee-1.onrender.com/oauth2/authorization/google";
}