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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private EmailService emailService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private NewsletterService newsletterService;

    private Map<String, String> otpStorage = new ConcurrentHashMap<>();

    // ✅ LOGIN
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

    // ✅ SEND OTP
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOTP(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String purpose = request.get("purpose");

        try {

            Optional<User> userOpt = userRepository.findByEmail(email);

            if ("registration".equals(purpose) && userOpt.isPresent())
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already exists"));

            if ("reset".equals(purpose) && userOpt.isEmpty())
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Email not found"));

            String otp = emailService.generateOTP();
            otpStorage.put(email, otp);

            emailService.sendOtpEmail(email, otp);

            return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));

        } catch (Exception e) {

            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");
        String otp = payload.get("otp");

        if (!otpStorage.containsKey(email) || !otpStorage.get(email).equals(otp))
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP"));

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

        boolean wantsNewsletter = "true".equalsIgnoreCase(payload.get("newsletterOptIn"));

        if (wantsNewsletter)
            newsletterService.subscribe(email, "signup", user.getId());

        return ResponseEntity.ok(Map.of("message", "Registration Successful"));
    }

    // ✅ UPDATE PROFILE
    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));

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

    // ✅ SUBSCRIPTION VERIFY
    @PostMapping("/subscription/verify")
    public ResponseEntity<?> verifySubscription(@RequestBody Map<String, Object> data) {

        try {

            String email = (String) data.get("email");
            String paymentId = (String) data.get("payment_id");
            String plan = ((String) data.get("plan")).toUpperCase();

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));

            User user = userOpt.get();

            if (plan.equals("PREMIUM")) {

                user.setSubscriptionPlan("PREMIUM");
                user.setSubscriptionStatus("ACTIVE");
                user.setTrending(true);
                user.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));

                Wallet adminWallet = walletRepository.findByUserEmail("admin@gmail.com")
                        .orElseGet(() -> walletRepository.save(new Wallet("admin@gmail.com", 0.0)));

                adminWallet.setBalance(adminWallet.getBalance() + 49);
                walletRepository.save(adminWallet);

                Transaction txn = new Transaction();
                txn.setSenderEmail(email);
                txn.setReceiverEmail("admin@gmail.com");
                txn.setAmount(49);
                txn.setTimestamp(LocalDateTime.now());

                transactionRepository.save(txn);

            } else {

                user.setSubscriptionPlan("STARTER");
                user.setSubscriptionStatus("ACTIVE");
                user.setTrending(false);
                user.setSubscriptionEndDate(null);
            }

            userRepository.save(user);

            return ResponseEntity.ok(Map.of("status", "success"));

        } catch (Exception e) {

            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ GOOGLE COMPLETE (🔥 MOST IMPORTANT FIX)
    @PostMapping("/google-complete")
    public ResponseEntity<?> completeGoogleRegistration(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String roleStr = request.get("role");

        try {

            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isEmpty())
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));

            User user = userOptional.get();

            user.setRole(User.Role.valueOf(roleStr.toUpperCase()));
            user.setEnabled(true);

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();

            response.put("email", user.getEmail());
            response.put("fullName", user.getFullName());
            response.put("mobile", user.getMobile());
            response.put("location", user.getLocation());
            response.put("profileImage", user.getProfileImage());
            response.put("role", user.getRole());
            response.put("subscriptionPlan", user.getSubscriptionPlan());
            response.put("subscriptionStatus", user.getSubscriptionStatus());
            response.put("isTrending", user.isTrending());

            return ResponseEntity.ok(response);

<<<<<<< HEAD
=======
            return ResponseEntity.ok(Map.of("message", "Support inquiry sent successfully"));
>>>>>>> da07b04 (New)
        } catch (Exception e) {

            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ GET USER (Session Restore)
    @GetMapping("/user")
    public ResponseEntity<?> getUser(@RequestParam String email) {

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty())
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));

        return ResponseEntity.ok(userOpt.get());
    }
}
