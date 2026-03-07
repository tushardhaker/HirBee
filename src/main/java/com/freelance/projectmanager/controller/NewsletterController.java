package com.freelance.projectmanager.controller;

import com.freelance.projectmanager.model.NewsletterSubscriber;
import com.freelance.projectmanager.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
// @CrossOrigin(origins = { "http://localhost:5500", "https://hir-bee-3nwb.vercel.app" }, allowCredentials = "true")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;
    private final ObjectMapper objectMapper;

    /**
     * Subscribe endpoint (unchanged)
     */
   @PostMapping("/subscribe")
public ResponseEntity<?> subscribe(@RequestBody Map<String, String> payload) {
    System.out.println("=== SUBSCRIBE CALLED ===");
    System.out.println("Payload received: " + payload);

    try {
        String email = payload.get("email");
        String source = payload.getOrDefault("source", "homepage");

        if (email == null || email.trim().isEmpty()) {
            System.out.println("Missing email");
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        System.out.println("Calling service → email: " + email + ", source: " + source);
        newsletterService.subscribe(email, source, null);

        System.out.println("Subscribe finished successfully");
        return ResponseEntity.ok(Map.of("message", "Subscribed successfully! You will receive updates."));
    } catch (IllegalArgumentException e) {
        System.out.println("Validation error: " + e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        System.err.println("CRITICAL ERROR in subscribe:");
        e.printStackTrace();
        return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
    }
}
    /**
     * Get all subscribers (unchanged)
     */
    @GetMapping("/all")
    public ResponseEntity<List<NewsletterSubscriber>> getAllSubscribers() {
        System.out.println("GET /api/newsletter/all called");
        try {
            List<NewsletterSubscriber> subs = newsletterService.getAllSubscribers();
            System.out.println("Found " + subs.size() + " subscribers");
            return ResponseEntity.ok(subs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * Send bulk email with optional file attachment
     * Accepts multipart/form-data (from your frontend FormData)
     */
    @PostMapping(value = "/send-bulk", consumes = "multipart/form-data")
    public ResponseEntity<String> sendBulkEmail(
            @RequestParam("emails") String emailsJson,
            @RequestParam("subject") String subject,
            @RequestParam("message") String message,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment) {

        try {
            // Parse JSON string of emails into List<String>
            List<String> emails = objectMapper.readValue(emailsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

            if (emails == null || emails.isEmpty()) {
                return ResponseEntity.badRequest().body("No emails provided");
            }

            if (subject == null || subject.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Subject is required");
            }

            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message is required");
            }

            // Call service with attachment (service handles null attachment)
            newsletterService.sendBulkNewsletter(emails, subject, message, attachment);

            return ResponseEntity.ok("Emails sent successfully to " + emails.size() + " subscribers");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to send emails: " + e.getMessage());
        }
    }
}