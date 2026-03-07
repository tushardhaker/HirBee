package com.freelance.projectmanager.controller;

import com.freelance.projectmanager.model.BankDetails;
import com.freelance.projectmanager.repository.BankDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank")
// @CrossOrigin(origins = { "http://localhost:5500", "https://hir-bee-3nwb.vercel.app" }, allowCredentials = "true")
public class BankDetailController {

    @Autowired 
    private BankDetailsRepository bankDetailsRepository;

    // Fetch Details: JS calls this on page load
    @GetMapping("/my-details")
    public ResponseEntity<?> getBankDetails(@RequestParam String email) {
        return bankDetailsRepository.findByUserEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(new BankDetails())); // Return empty object if not found
    }

    // Save/Update Details: JS calls this on Form Submit
    @PostMapping("/save")
    public ResponseEntity<?> saveBankDetails(@RequestBody BankDetails details) {
        try {
            // Check if record already exists for this email
            BankDetails existing = bankDetailsRepository.findByUserEmail(details.getUserEmail())
                    .orElse(new BankDetails());
            
            // Update fields
            existing.setUserEmail(details.getUserEmail());
            existing.setAccountHolderName(details.getAccountHolderName());
            existing.setUpiId(details.getUpiId());
            existing.setAccountNumber(details.getAccountNumber());
            existing.setIfscCode(details.getIfscCode());

            bankDetailsRepository.save(existing);
            return ResponseEntity.ok("Bank details saved successfully in database!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving to DB: " + e.getMessage());
        }
    }
}