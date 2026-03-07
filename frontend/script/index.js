// Data synchronized with your MySQL Database structure (budget, details, location)
const projects = [
    { details: "Frontend Developer", location: "Jaipur Municipal Corporation", mode: "ONLINE", budget: "30000" },
    { details: "Backend Developer", location: "GLOBAL", mode: "OFFLINE", budget: "60000" },
    { details: "Full Stack Developer", location: "GLOBAL", mode: "OFFLINE", budget: "80000" },
    { details: "OS Expert", location: "Remote", mode: "OFFLINE", budget: "35000" }
];

/**
 * Renders the trending projects onto the dashboard
 * Uses the .project-card and .project-budget-label classes from your CSS
 */
function renderProjects() {
    const container = document.getElementById('projectContainer');
    if (!container) return;

    container.innerHTML = projects.map(p => `
        <div class="col-md-4">
            <div class="project-card shadow-sm">
                <div class="d-flex justify-content-between align-items-start mb-3">
                    <div class="title-accent"></div>
                    <span class="project-budget-label">₹${p.budget}</span>
                </div>
                <h5 class="fw-bold mb-2">${p.details}</h5>
                <p class="project-location-text mb-3">
                    <i class="bi bi-geo-alt-fill me-1"></i> ${p.location} (${p.mode})
                </p>
                <div class="d-flex justify-content-between align-items-center mt-auto">
                    <span class="badge rounded-pill bg-light text-primary border">Verified</span>
                    <button class="btn btn-sm btn-brand-primary px-4 rounded-pill" onclick="redirectToSignup()">
                        Bid Now
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

// Search Logic
function performSearch() {
    const category = document.getElementById('categoryFilter')?.value || '';
    const location = document.getElementById('locationFilter')?.value.trim() || '';
    const keywords = document.getElementById('topicFilter')?.value.trim() || '';

    // Reference ke saath URL banao
    const params = new URLSearchParams();
    if (category) params.append('category', category);
    if (location) params.append('location', location);
    if (keywords) params.append('keywords', keywords);

    const queryString = params.toString();
    const redirectUrl = `total_jobs.html${queryString ? '?' + queryString : ''}`;

    // Redirect kar do total_jobs.html pe
    window.location.href = redirectUrl;
}

// Navigation Functions
function redirectToLogin() {
    window.location.href = "login.html";
}

function redirectToSignup() {
    window.location.href = "signup.html";
}

function goToAbout() {
    window.location.href = "about.html";
}

function goToJobs() {
    window.location.href = "total_jobs.html";
}

// Initialize the page components
document.addEventListener('DOMContentLoaded', () => {
    // Render the project cards
    renderProjects();
    
    // Attach event listener to search button
    const searchBtn = document.querySelector('.search-container button');
    if(searchBtn) {
        searchBtn.addEventListener('click', performSearch);
    }
});