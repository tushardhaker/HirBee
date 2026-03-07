package com.freelance.projectmanager.controller;

import com.freelance.projectmanager.model.Proposal;
import com.freelance.projectmanager.model.User;
import com.freelance.projectmanager.repository.ProposalRepository;
import com.freelance.projectmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/proposals")
// @CrossOrigin(origins = { "http://localhost:5500", "https://hir-bee-3nwb.vercel.app" }, allowCredentials = "true")
public class ProposalController {

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private UserRepository userRepository;

   @PostMapping("/submit")
public ResponseEntity<?> submitProposal(@RequestBody Proposal proposal) {
    try {
        String freelancerEmail = proposal.getFreelancerEmail();
        if (freelancerEmail == null || freelancerEmail.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Freelancer email is missing.");
        }

        // Find user
        Optional<User> userOpt = userRepository.findByEmail(freelancerEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found. Please login again.");
        }

        User user = userOpt.get();
        String subscription = user.getSubscriptionPlan(); // Assuming this returns "FREE", "PREMIUM", etc.

        // Free users (or no subscription) → max 7 bids per week
        if (subscription == null || 
            "FREE".equalsIgnoreCase(subscription) || 
            "NONE".equalsIgnoreCase(subscription)) {
            
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long bidCount = proposalRepository.countBidsInLastWeek(freelancerEmail, weekAgo);

            if (bidCount >= 7) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Weekly bid limit reached! Free users can only submit 7 proposals per week. Upgrade to Premium for unlimited bids.");
            }
        }
        // Premium / subscribed users → no limit

        // Proceed with submission
        proposal.setStatus("PENDING");
        if (proposal.getCreatedAt() == null) {
            proposal.setCreatedAt(LocalDateTime.now());
        }

        Proposal saved = proposalRepository.save(proposal);
        return ResponseEntity.ok(saved);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error submitting proposal: " + e.getMessage());
    }
}

    @GetMapping("/my-list")
    public ResponseEntity<List<Proposal>> getMyProposals(@RequestParam String email) {
        return ResponseEntity.ok(proposalRepository.findByFreelancerEmail(email));
    }

    @GetMapping("/client-list")
    public ResponseEntity<List<Proposal>> getClientProposals(@RequestParam String email) {
        return ResponseEntity.ok(proposalRepository.findByClientEmail(email));
    }

    @PostMapping("/update-status")
    public ResponseEntity<?> updateStatus(@RequestParam Long id, @RequestParam String status) {
        Optional<Proposal> opt = proposalRepository.findById(id);
        if (opt.isPresent()) {
            Proposal p = opt.get();
            p.setStatus(status);
            proposalRepository.save(p);
            return ResponseEntity.ok("Status updated.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Proposal not found.");
    }
}