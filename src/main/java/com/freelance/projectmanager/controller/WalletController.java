package com.freelance.projectmanager.controller;

import com.freelance.projectmanager.model.*;
import com.freelance.projectmanager.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
// @CrossOrigin(origins = { "http://localhost:5500", "https://hir-bee-3nwb.vercel.app" }, allowCredentials = "true")
public class WalletController {

    private static final String ADMIN_EMAIL = "admin@gmail.com";

    @Autowired private WalletRepository walletRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private BankDetailsRepository bankDetailsRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactionHistory(@RequestParam String email) {
        List<Transaction> transactions = transactionRepository.findBySenderEmailOrReceiverEmail(email, email);
        List<Transaction> sortedTxs = transactions.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(sortedTxs);
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestParam String email) {
        Wallet wallet = walletRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUserEmail(email);
                    newWallet.setBalance(0.0);
                    return walletRepository.save(newWallet);
                });
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/add-money")
    @Transactional
    public ResponseEntity<?> addMoney(@RequestBody Map<String, Object> data) {
        String email = (String) data.get("email");
        Double amount = Double.parseDouble(data.get("amount").toString());

        Wallet clientWallet = walletRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
        clientWallet.setBalance(clientWallet.getBalance() + amount);
        walletRepository.save(clientWallet);

        updateAdminEscrowBalance(amount);
        saveTransaction("RAZORPAY", email, amount);
        return ResponseEntity.ok(clientWallet);
    }

    @PostMapping("/subscription/verify")
    @Transactional
    public ResponseEntity<?> verifySubscription(@RequestBody Map<String, Object> data) {
        try {
            String email = (String) data.get("email");
            String plan = (String) data.get("plan");

            // Debugging Logs
            System.out.println("Received Subscription Request:");
            System.out.println("Email: " + email);
            System.out.println("Plan: " + plan);
            System.out.println("Full Data: " + data);

            Object amountObj = data.get("amount");
            Double amount = 0.0;
            if (amountObj != null) {
                amount = Double.parseDouble(amountObj.toString());
            }

            // Agar amount 0 aa rahi hai toh fallback (Backup logic)
            if (amount <= 0) {
                if ("SILVER".equalsIgnoreCase(plan)) amount = 199.0;
                else if ("GOLD".equalsIgnoreCase(plan)) amount = 499.0;
                else if ("DIAMOND".equalsIgnoreCase(plan)) amount = 999.0;
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setSubscriptionStatus("PENDING_APPROVAL");
                user.setSubscriptionPlan(plan);
                userRepository.save(user);

                updateAdminEscrowBalance(amount);
                saveTransaction(email, "ADMIN_SUBSCRIPTION_" + plan, amount);

                System.out.println("Final Amount Added to Admin: " + amount);

                return ResponseEntity.ok(Map.of("status", "success", "message", "Payment Added to Admin Bank"));
            }
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    @Transactional
    public ResponseEntity<?> transferMoney(@RequestBody Map<String, Object> payload) {
        String senderEmail = (String) payload.get("senderEmail");
        String receiverEmail = (String) payload.get("receiverEmail");
        Double amount = Double.valueOf(payload.get("amount").toString());

        Wallet sender = walletRepository.findByUserEmail(senderEmail).orElseThrow();
        Wallet receiver = walletRepository.findByUserEmail(receiverEmail)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUserEmail(receiverEmail);
                    w.setBalance(0.0);
                    return walletRepository.save(w);
                });

        if (sender.getBalance() < amount) return ResponseEntity.badRequest().body("Insufficient Funds");

        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);

        walletRepository.save(sender);
        walletRepository.save(receiver);

        saveTransaction(senderEmail, receiverEmail, amount);
        return ResponseEntity.ok("Transfer Successful");
    }

    @PostMapping("/withdraw")
    @Transactional
    public ResponseEntity<?> withdrawMoney(@RequestBody Map<String, Object> payload) {
        String email = (String) payload.get("email");
        Double requestedAmount = Double.valueOf(payload.get("amount").toString());

        Wallet freelancerWallet = walletRepository.findByUserEmail(email).orElseThrow();
        if (freelancerWallet.getBalance() < requestedAmount) return ResponseEntity.badRequest().body("Insufficient Tokens");

        bankDetailsRepository.findByUserEmail(email).orElseThrow(() -> new RuntimeException("No bank details found."));

        double platformFeeRate = 0.10;
        double platformFee = requestedAmount * platformFeeRate;
        double netAmountToUser = requestedAmount - platformFee;

        freelancerWallet.setBalance(freelancerWallet.getBalance() - requestedAmount);
        walletRepository.save(freelancerWallet);

        updateAdminEscrowBalance(-netAmountToUser);
        saveTransaction(email, "REAL_BANK_OUT_NET_" + netAmountToUser + "_FEE_" + platformFee, requestedAmount);

        return ResponseEntity.ok("Withdrawal Processed: ₹" + netAmountToUser);
    }

    private void updateAdminEscrowBalance(Double amount) {
        Wallet adminWallet = walletRepository.findByUserEmail(ADMIN_EMAIL)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUserEmail(ADMIN_EMAIL);
                    w.setBalance(0.0);
                    return walletRepository.save(w);
                });
        adminWallet.setBalance(adminWallet.getBalance() + amount);
        walletRepository.save(adminWallet);
    }

    private void saveTransaction(String sender, String receiver, Double amount) {
        Transaction tx = new Transaction();
        tx.setSenderEmail(sender);
        tx.setReceiverEmail(receiver);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        transactionRepository.save(tx);
    }
}