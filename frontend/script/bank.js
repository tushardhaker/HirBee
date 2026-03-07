document.addEventListener('DOMContentLoaded', async function() {
    const userEmail = localStorage.getItem('userEmail');
    
    // 1. Security Check
    if (!userEmail) {
        window.location.href = 'login.html';
        return;
    }

    // 2. Load Existing Data from Backend
    try {
        const response = await fetch(`https://hirbee-1.onrender.com/api/bank/my-details?email=${encodeURIComponent(userEmail)}`);
        
        if (response.ok) {
            const data = await response.json();
            
            if (data) {
                document.getElementById('holderName').value = data.accountHolderName || "";
                document.getElementById('upiId').value = data.upiId || "";
                document.getElementById('accNo').value = data.accountNumber || "";
                document.getElementById('ifsc').value = data.ifscCode || "";
            }
        }
    } catch (error) {
        console.error("Error fetching bank details:", error);
    }
});

/**
 * Modern Toast Notification System (Replacing alert)
 */
function showNotify(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `custom-toast toast-${type}`;
    
    const icon = type === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill';
    
    toast.innerHTML = `
        <i class="bi ${icon} fs-5"></i>
        <div>
            <div class="fw-bold small text-dark">${type === 'success' ? 'Success' : 'Error'}</div>
            <div class="text-muted small">${message}</div>
        </div>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('toast-fade-out');
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

/**
 * 3. IMPROVED REDIRECT FUNCTION
 */
function goBackToDashboard() {
    const role = localStorage.getItem('userRole'); 
    
    if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
        window.location.href = 'admin-dashboard.html';
    } else if (role === 'CLIENT') {
        window.location.href = 'client-dashboard.html';
    } else {
        window.location.href = 'freelancer-dashboard.html';
    }
}

// 4. Save/Update Bank Details
document.getElementById('bankDetailsForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const btn = document.getElementById('submitBtn');
    const btnLabel = document.getElementById('btnLabel');

    // UI: Loading State
    btn.disabled = true;
    btnLabel.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>Saving...`;

    const payload = {
        userEmail: localStorage.getItem('userEmail'),
        accountHolderName: document.getElementById('holderName').value,
        upiId: document.getElementById('upiId').value,
        accountNumber: document.getElementById('accNo').value,
        ifscCode: document.getElementById('ifsc').value
    };

    try {
        const response = await fetch('https://hirbee-1.onrender.com/api/bank/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            showNotify("Payment details updated successfully!", "success");
            // Automatically send back after a short delay
            setTimeout(() => goBackToDashboard(), 2000);
        } else {
            const errorMsg = await response.text();
            showNotify("Failed to save: " + errorMsg, "error");
            btn.disabled = false;
            btnLabel.innerHTML = `<i class="bi bi-check2-circle me-2"></i>Update Payment Details`;
        }
    } catch (error) {
        console.error("Save Error:", error);
        showNotify("Server error. Please check if backend is running.", "error");
        btn.disabled = false;
        btnLabel.innerHTML = `<i class="bi bi-check2-circle me-2"></i>Update Payment Details`;
    }
});