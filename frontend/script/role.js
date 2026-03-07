/**
 * Role Selection Logic for HirBee
 * Handles Google Registration completion.
 */

const API_URL = "https://hirbee-1.onrender.com/api/auth";

async function selectRole(role) {
    // 1. Validation: Terms check
    const termsChecked = document.getElementById('terms').checked;
    if (!termsChecked) {
        alert("Please accept the Terms & Conditions to proceed.");
        return;
    }

    // 2. Extract Email from URL
    const urlParams = new URLSearchParams(window.location.search);
    const email = urlParams.get('google_email');

    if (!email) {
        alert("Session error: No email found. Please login with Google again.");
        window.location.href = "signup.html";
        return;
    }

    // Show loading
    const loader = document.getElementById('loading');
    if (loader) loader.style.display = 'block';

    try {
        // 3. API Call
        const response = await fetch(`${API_URL}/google-complete`, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ 
                email: email, 
                role: role.toUpperCase() 
            })
        });

        // Response check
        const data = await response.json();

        if (response.ok) {
            alert("Registration Complete! Your account is now active.");
            window.location.href = "login.html";
        } else {
            // Ab ye crash nahi hoga kyunki backend JSON bhej raha hai
            console.error("Server Error Details:", data);
            alert("Error: " + (data.message || "Failed to save role."));
        }
    } catch (error) {
        console.error("Fetch Connection Error:", error);
        alert("Server connection error. Please ensure Spring Boot is running.");
    } finally {
        if (loader) loader.style.display = 'none';
    }
}

// Optional selection visual effect
document.querySelectorAll('.role-option').forEach(card => {
    card.addEventListener('click', function() {
        document.querySelectorAll('.role-option').forEach(c => c.style.border = "none");
        this.style.border = "2px solid #6366f1";
    });
});