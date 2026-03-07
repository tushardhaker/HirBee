// tracker.js - Sabhi pages par ise include karein
const TRACKER_API = 'https://hirbee-1.onrender.com/api/admin';

async function sendLog(type, message) {
    const userEmail = localStorage.getItem('userEmail') || 'Guest';
    const userRole = localStorage.getItem('userRole') || 'GUEST';

    const payload = {
        userEmail: userEmail,
        userRole: userRole,
        activityType: type,
        description: message,
        pageUrl: window.location.pathname
    };

    try {
        // Beacon API ka use behtar hota hai page exit ke liye, par hum fetch use kar rahe hain simple rakhne ke liye
        fetch(`${TRACKER_API}/log-activity`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
            keepalive: true // Ye page close hote waqt bhi request bhejta hai
        });
    } catch (err) {
        console.error("Tracking Error:", err);
    }
}

// 1. Detect Clicks (Job Post, Add Money, etc.)
document.addEventListener('click', (e) => {
    const target = e.target.closest('button') || e.target.closest('.btn') || e.target.closest('a');
    if (target) {
        const text = target.innerText.trim() || target.getAttribute('title') || "Link/Button";
        sendLog('CLICK', `User clicked: ${text}`);
    }
});

// 2. Detect "Back" or "Exit" (Abandoned Task)
// Jab user tab switch kare ya page leave kare
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
        // Agar user kisi form page par tha (e.g., job post)
        if (window.location.href.includes('post') || window.location.href.includes('wallet')) {
            sendLog('ABANDONED', 'User left the process/page without completing');
        } else {
            sendLog('EXIT', 'User closed the tab or switched');
        }
    }
});

// 3. Detect Page Load
window.addEventListener('load', () => {
    sendLog('VISIT', `User landed on ${window.location.pathname}`);
});