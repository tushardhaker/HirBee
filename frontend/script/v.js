/**
 * Freelancer Dashboard Logic
 */

window.addEventListener('load', function() {
    // Check if the user has already seen the dashboard tour
    if (!localStorage.getItem('dashboard_tour_done')) {
        startDashboardTour();
    }
});

function startDashboardTour() {
    introJs().setOptions({
        steps: [
            {
                title: 'Welcome to your Hub! 🚀',
                intro: 'This is your command center. Let us show you where everything is.'
            },
            {
                element: '#welcomeHeader',
                intro: 'Here you can see your current status and greetings.',
                position: 'bottom'
            },
            {
                element: '#statsOverview',
                intro: 'Monitor your active projects, earnings, and profile growth at a glance.',
                position: 'bottom'
            },
            {
                element: '#findJobsStep',
                intro: 'Looking for work? Browse thousands of open projects here.',
                position: 'right'
            },
            {
                element: '#myProjectsStep',
                intro: 'Manage your ongoing contracts and track your milestones here.',
                position: 'right'
            },
            {
                element: '#jobFeedStep',
                intro: 'We curate these jobs based on your specific skills and interests.',
                position: 'top'
            },
            {
                element: '#profileStep',
                intro: 'Keep your portfolio updated to attract high-paying clients.',
                position: 'left'
            }
        ],
        showProgress: true,
        exitOnOverlayClick: false,
        nextLabel: 'Next →',
        prevLabel: '← Back',
        skipLabel: 'Skip',
        doneLabel: 'Got it!'
    }).oncomplete(function() {
        // Set the flag so it doesn't show again
        localStorage.setItem('dashboard_tour_done', 'true');
    }).onexit(function() {
        // Also set the flag if they exit via the "X" or "Skip"
        localStorage.setItem('dashboard_tour_done', 'true');
    }).start();
}

/**
 * Add your existing dashboard features below (API calls, data fetching, etc.)
 */
console.log("Dashboard features initialized...");