package com.freelance.projectmanager.controller;

import com.freelance.projectmanager.model.Transaction;
import com.freelance.projectmanager.model.User;
import com.freelance.projectmanager.model.Wallet;
import com.freelance.projectmanager.repository.TransactionRepository;
import com.freelance.projectmanager.repository.UserRepository;
import com.freelance.projectmanager.repository.WalletRepository;
import com.freelance.projectmanager.service.EmailService;
import com.freelance.projectmanager.service.NewsletterService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = { "http://localhost:5500", "http://127.0.0.1:5500" })
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NewsletterService newsletterService;

    private Map<String, String> otpStorage = new ConcurrentHashMap<>();

    // 1. LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                if (!user.isEnabled()) {
                    return ResponseEntity.status(403).body(Map.of("error", "Account is banned"));
                }
                return ResponseEntity.ok(user);
            }
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    // 2. SEND OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String purpose = request.get("purpose");

        Optional<User> userOpt = userRepository.findByEmail(email);

        if ("registration".equals(purpose) && userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already exists!"));
        }
        if ("reset".equals(purpose) && userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Email not found!"));
        }

        String otp = emailService.generateOTP();
        otpStorage.put(email, otp);
        emailService.sendOtpEmail(email, otp);

        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    // 3. REGISTER
    // ... existing imports ...

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");

        if (!otpStorage.containsKey(email) || !otpStorage.get(email).equals(otp)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));
        }

        User user = new User();
        user.setFullName(payload.get("fullName"));
        user.setEmail(email);
        user.setMobile(payload.get("mobile"));
        user.setPassword(payload.get("password"));
        user.setRole(User.Role.valueOf(payload.get("role").toUpperCase()));
        user.setEnabled(true);
        user.setSubscriptionPlan("FREE");
        user.setSubscriptionStatus("NONE");
        user.setTrending(false);

        userRepository.save(user);
        otpStorage.remove(email);

        // ────────────────────────────────────────────────
        // Newsletter subscription check (naya code)
        // ────────────────────────────────────────────────
        boolean wantsNewsletter = "true".equalsIgnoreCase(payload.get("newsletterOptIn"));
        if (wantsNewsletter) {
            newsletterService.subscribe(email, "signup", user.getId());
        }

        return ResponseEntity.ok(Map.of("message", "Registration Successful!"));
    }

    // 4. UPDATE PROFILE
    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (payload.containsKey("fullName"))
                user.setFullName(payload.get("fullName"));
            if (payload.containsKey("mobile"))
                user.setMobile(payload.get("mobile"));
            if (payload.containsKey("location"))
                user.setLocation(payload.get("location"));
            if (payload.containsKey("profileImage"))
                user.setProfileImage(payload.get("profileImage"));

            userRepository.save(user);
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    // 5. SUBSCRIBE – Updated for 0 Rs and 49 Rs plans only
    @PostMapping("/subscription/verify")
    public ResponseEntity<?> verifySubscription(@RequestBody Map<String, Object> data) {
        try {
            String email = (String) data.get("email");
            String paymentId = (String) data.get("payment_id"); // Starter ke liye null ho sakta hai
            String plan = (String) data.get("plan");

            if (email == null || plan == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Missing required fields: email and plan"));
            }

            // Updated Amount mapping – Only 0 and 49
            double amount;
            String planUpper = plan.toUpperCase();
            
            if (planUpper.equals("STARTER")) {
                amount = 0.0;
            } else if (planUpper.equals("PREMIUM")) {
                amount = 49.0;
                // Premium ke liye payment_id mandatory hai
                if (paymentId == null || paymentId.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Payment ID is required for Premium plan"));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid plan: " + plan + ". Only STARTER and PREMIUM are allowed."));
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();

            // Check if already subscribed to Premium (Starter users can upgrade)
            if ("ACTIVE".equalsIgnoreCase(user.getSubscriptionStatus()) && "PREMIUM".equalsIgnoreCase(user.getSubscriptionPlan())) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "User already has an active PREMIUM subscription."));
            }

            // Activate subscription logic
            user.setSubscriptionPlan(planUpper);
            user.setSubscriptionStatus("ACTIVE");
            
            // Premium users ko trending feature aur 1 month validity milti hai
            if (planUpper.equals("PREMIUM")) {
                user.setTrending(true);
                user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));
            } else {
                user.setTrending(false); // Starter ko trending nahi milti
                user.setSubscriptionEndDate(null); // Lifetime for free
            }
            
            userRepository.save(user);

            // Wallet aur Transaction update sirf paid plans ke liye
            if (amount > 0) {
                // Add to admin wallet
                Optional<Wallet> walletOpt = walletRepository.findByUserEmail("admin@gmail.com");
                Wallet adminWallet = walletOpt.orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUserEmail("admin@gmail.com");
                    w.setBalance(0.0);
                    return walletRepository.save(w);
                });

                adminWallet.setBalance(adminWallet.getBalance() + amount);
                walletRepository.save(adminWallet);

                // Save transaction
                Transaction txn = new Transaction();
                txn.setSenderEmail(email);
                txn.setReceiverEmail("admin@gmail.com");
                txn.setAmount(amount);
                txn.setTimestamp(LocalDateTime.now());
                transactionRepository.save(txn);
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Premium activated! ₹" + amount + " added to admin wallet.",
                    "paymentId", paymentId,
                    "newAdminBalance", adminWallet.getBalance()));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Starter plan activated successfully."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Server error",
                    "details", e.getMessage()));
        }
    }

    // 6. RESET PASSWORD
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");

        if (otpStorage.containsKey(email) && otpStorage.get(email).equals(otp)) {
            User user = userRepository.findByEmail(email).get();
            user.setPassword(request.get("newPassword"));
            userRepository.save(user);
            otpStorage.remove(email);
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));
    }

    // 7. FREELANCERS / USERS BY ROLE (Trending first)
    @GetMapping("/freelancers-by-location")
    public ResponseEntity<?> getFreelancersByLocation(@RequestParam String location) {
        try {
            List<User> freelancers = userRepository.findByRoleAndLocationWithRanking(User.Role.FREELANCER, location);
            return ResponseEntity.ok(freelancers);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/users-by-role")
    public ResponseEntity<?> getUsersByRole(@RequestParam String role) {
        try {
            User.Role userRole = User.Role.valueOf(role.toUpperCase());
            List<User> users = userRepository.findAllByRoleOrderByTrending(userRole);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // 8. GOOGLE COMPLETE
    @PostMapping("/google-complete")
    public ResponseEntity<?> completeGoogleRegistration(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String roleStr = request.get("role");

        try {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setRole(User.Role.valueOf(roleStr.toUpperCase()));
                user.setEnabled(true);
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("status", "success", "message", "Role updated successfully"));
            }
            return ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // 9. GET CURRENT USER (for frontend refresh)
    @GetMapping("/user")
    public ResponseEntity<?> getUser(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("subscription_plan", user.getSubscriptionPlan());
            response.put("subscription_status", user.getSubscriptionStatus());
            response.put("subscription_end_date", user.getSubscriptionEndDate());
            response.put("is_trending", user.isTrending());
            response.put("role", user.getRole().toString());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    // 10. ADMIN REMOVE SUBSCRIPTION (Blue Tick Remove)
    @DeleteMapping("/admin/remove-subscription")
    public ResponseEntity<?> removeSubscription(@RequestParam String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setSubscriptionPlan("FREE");
            user.setSubscriptionStatus("NONE");
            user.setTrending(false);
            user.setSubscriptionEndDate(null);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Premium status removed. Trending OFF."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 11. ADMIN: GET ALL SUBSCRIBED USERS
    @GetMapping("/admin/subscriptions")
    public ResponseEntity<List<User>> getAllSubscriptions() {
        List<User> subscribedUsers = userRepository.findAll()
                .stream()
                .filter(user -> user.getSubscriptionPlan() != null && !user.getSubscriptionPlan().equals("FREE"))
                .collect(Collectors.toList());
        return ResponseEntity.ok(subscribedUsers);
    }

    // 12. CONTACT SUPPORT - Sends email to Company Admin
    @PostMapping("/contact")
    public ResponseEntity<?> contactSupport(@RequestBody Map<String, String> payload) {
        try {
            String userName = payload.get("name");
            String userEmail = payload.get("email");
            String subject = payload.get("subject");
            String messageBody = payload.get("message");

            if (userEmail == null || messageBody == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and Message are required"));
            }

            // Design the email body for the Admin/Company
            String adminEmailBody = String.format(
                    "New Support Inquiry Received:\n\n" +
                            "From: %s (%s)\n" +
                            "Subject: %s\n\n" +
                            "Message:\n%s\n\n" +
                            "--- End of Message ---",
                    userName, userEmail, subject, messageBody);

            // Send to your company's registered email
            emailService.sendNoticeEmail(
                    "coccrv109@gmail.com", // Your company/admin email
                    "Contact Form: " + subject,
                    adminEmailBody);

            return ResponseEntity.ok(Map.of("message", "Support inquiry sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Could not send email", "details", e.getMessage()));
        }
    }
}