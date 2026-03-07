/**
 * Modern Toast Notification Function
 */
function showNotify(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return; 

    const toast = document.createElement('div');
    toast.className = `custom-toast toast-${type}`;
    
    // Icon based on type
    const icon = type === 'success' ? 'bi-check-circle-fill text-success' : 'bi-exclamation-triangle-fill text-danger';
    
    toast.innerHTML = `
        <i class="bi ${icon} fs-5"></i>
        <div>
            <div class="fw-bold small text-dark">${type === 'success' ? 'Success' : 'Attention'}</div>
            <div class="text-muted small">${message}</div>
        </div>
    `;
    
    container.appendChild(toast);

    // Auto remove after 3.5 seconds
    setTimeout(() => {
        toast.classList.add('toast-fade-out');
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// Location Toggle Logic
document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.getElementById('locationToggle');
    const display = document.getElementById('locationDisplay');
    const cityBadge = document.getElementById('currentCityBadge');
    
    // Dashboard se saved location uthao
    const savedLocation = localStorage.getItem('location') || "Unknown";

    if (toggle) {
        toggle.addEventListener('change', () => {
            if (toggle.checked) {
                display.classList.remove('d-none');
                cityBadge.innerHTML = `<i class="bi bi-geo-alt-fill"></i> Visibility: ${savedLocation}`;
            } else {
                display.classList.add('d-none');
            }
        });
    }
});

// Form Submission Logic
document.getElementById('createJobForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const isLocalOnly = document.getElementById('locationToggle').checked;
    const savedLocation = localStorage.getItem('location');
    const submitBtn = document.getElementById('submitBtn');
    const btnLabel = document.getElementById('btnLabel');

    // Updated Payload to include "title" AND "category"
    const payload = {
        title: document.getElementById('jobTitle').value,
        category: document.getElementById('jobCategory').value, // <-- New Category Field
        clientEmail: localStorage.getItem('userEmail'),
        details: document.getElementById('jobDetails').value,
        budget: parseFloat(document.getElementById('budget').value),
        mode: document.getElementById('workMode').value,
        // Agar toggle ON hai toh city bhejo, nahi toh "GLOBAL" bhejo
        location: isLocalOnly ? (savedLocation || "Remote") : "GLOBAL"
    };

    // UI Feedback
    if (submitBtn) {
        submitBtn.disabled = true;
        btnLabel.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>Posting...`;
    }

    try {
        const response = await fetch('https://hirbee-1.onrender.com/api/jobs/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            showNotify("Job Posted Successfully!", "success");
            
            setTimeout(() => {
                window.location.href = "client-dashboard.html";
            }, 2000);
        } else {
            const errorData = await response.json();
            showNotify(errorData.error || "Error posting job. Please try again.", "error");
            resetButton(submitBtn, btnLabel);
        }
    } catch (err) {
        showNotify("Server connection failed. Check if backend is running.", "error");
        resetButton(submitBtn, btnLabel);
    }
});

function resetButton(btn, label) {
    if (btn) {
        btn.disabled = false;
        label.innerHTML = `<i class="bi bi-rocket-takeoff me-2"></i>Post Job Now`;
    }
}