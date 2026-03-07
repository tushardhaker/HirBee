package com.freelance.projectmanager.controller;

import com.freelance.projectmanager.model.*;
import com.freelance.projectmanager.repository.*;
import com.freelance.projectmanager.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
// @CrossOrigin(origins = { "http://localhost:5500", "https://hir-bee-3nwb.vercel.app" }, allowCredentials = "true")
public class AdminController {

    private static final String ADMIN_EMAIL = "admin@gmail.com";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserActivityRepository activityRepository;

   @PostMapping("/send-notice")
public ResponseEntity<?> sendNotice(@RequestBody Map<String, String> request) {
    try {
        String toEmail = request.get("email");
        String subject = request.get("subject");
        String body = request.get("body");

        // Basic validation
        if (toEmail == null || !toEmail.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or missing email"));
        }
        if (subject == null || subject.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Subject is required"));
        }
        if (body == null || body.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message body is required"));
        }

        System.out.println("[ADMIN NOTICE] Sending to: " + toEmail);
        System.out.println("Subject: " + subject);
        System.out.println("Body preview: " + body.substring(0, Math.min(100, body.length())) + "...");

        emailService.sendNoticeEmail(toEmail, subject, body);

        System.out.println("[ADMIN NOTICE] Successfully sent to " + toEmail);

        return ResponseEntity.ok(Map.of("message", "Notice sent successfully!"));

    } catch (Exception e) {
        // Print FULL stack trace to console/logs — very important!
        e.printStackTrace();

        String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
        return ResponseEntity.status(500).body(Map.of(
            "error", "Failed to send notice",
            "details", errorMsg
        ));
    }
}

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalFreelancers", userRepository.countByRole(User.Role.FREELANCER));
        stats.put("totalJobs", jobRepository.count());
        stats.put("totalLocations", userRepository.countDistinctLocation());

        // Force fresh fetch from DB — no caching
        double escrowBalance = walletRepository.findByUserEmail(ADMIN_EMAIL)
                .map(Wallet::getBalance)
                .orElse(0.0);
        stats.put("adminWalletBalance", escrowBalance);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users")
    public List<User> getAllUsers(@RequestParam(required = false) String location) {
        if (location != null && !location.isEmpty()) {
            return userRepository.findByLocationIgnoreCase(location);
        }
        return userRepository.findAll();
    }

    @GetMapping("/payments")
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @GetMapping("/jobs")
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @PostMapping("/user/toggle-ban")
    public ResponseEntity<?> toggleBan(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "User status updated"));
    }

    @PostMapping("/subscription/update-status")
    public ResponseEntity<?> updateSubscriptionStatus(@RequestParam String email, @RequestParam String status) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setSubscriptionStatus(status);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Subscription " + status));
        }
        return ResponseEntity.status(404).body("User not found");
    }

    @PostMapping("/approve-subscription")
    public ResponseEntity<?> approveSubscription(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setSubscriptionStatus("ACTIVE"); // Direct Active
            user.setTrending(true);
            user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));

            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User is now ACTIVE and Trending."));
        }
        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
    }

    // ACTIVITY LOGGING
    @PostMapping("/log-activity")
    public ResponseEntity<?> logActivity(@RequestBody UserActivity activity) {
        activity.setTimestamp(LocalDateTime.now());
        activityRepository.save(activity);
        return ResponseEntity.ok(Map.of("message", "Activity logged"));
    }

    @GetMapping("/activities")
    public List<UserActivity> getAllActivities() {
        return activityRepository.findAllByOrderByTimestampDesc();
    }

    @DeleteMapping("/activities/clear")
    public ResponseEntity<?> clearActivities() {
        activityRepository.deleteAll();
        return ResponseEntity.ok(Map.of("message", "Logs cleared"));
    }
    
}