package com.freelance.projectmanager.service;

import com.freelance.projectmanager.model.NewsletterSubscriber;
import com.freelance.projectmanager.model.User;
import com.freelance.projectmanager.repository.NewsletterSubscriberRepository;
import com.freelance.projectmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Subscribe email to newsletter list
     * Ignores duplicates silently
     * Sends notification to admin
     */
    public void subscribe(String email, String source, Long userId) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        email = email.trim().toLowerCase();

        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Invalid email address");
        }

        // Silent return if already subscribed
        if (subscriberRepository.existsByEmail(email)) {
            return;
        }

        NewsletterSubscriber sub = NewsletterSubscriber.builder()
                .email(email)
                .source(source != null ? source : "unknown")
                .userId(userId)
                .subscribedAt(LocalDateTime.now())
                .active(true)
                .build();

        subscriberRepository.save(sub);

        // Admin notification
        String adminBody = String.format(
            "New Newsletter Subscriber Added!\n\n" +
            "Email       : %s\n" +
            "Source      : %s\n" +
            "User ID     : %s\n" +
            "Subscribed  : %s\n\n" +
            "You can now send trending news, job alerts, and benefits to this email.",
            email,
            sub.getSource(),
            userId != null ? userId : "Not registered (guest)",
            sub.getSubscribedAt()
        );

        emailService.sendNoticeEmail(
            "coccrv109@gmail.com",
            "New Newsletter Subscription - HirBee",
            adminBody
        );
    }

    public boolean isSubscribed(String email) {
        if (email == null) return false;
        return subscriberRepository.findByEmail(email.trim().toLowerCase())
                .map(NewsletterSubscriber::isActive)
                .orElse(false);
    }

    /**
     * Get all newsletter subscribers with proper role from User table
     */
    public List<NewsletterSubscriber> getAllSubscribers() {
        System.out.println("[DEBUG] getAllSubscribers() called - fetching from DB...");

        List<NewsletterSubscriber> subscribers = subscriberRepository.findAll();

        // Enrich each subscriber with role from User
        for (NewsletterSubscriber sub : subscribers) {
            if (sub.getUserId() != null) {
                userRepository.findById(sub.getUserId()).ifPresent(user -> {
                    // Set role from User entity (assuming User has getRole() returning enum)
                    sub.setRole(user.getRole() != null ? user.getRole().name() : null);
                });
            }
        }

        System.out.println("[DEBUG] Found " + subscribers.size() + " subscribers with roles enriched");
        return subscribers;
    }

    /**
 * Send bulk newsletter to multiple emails with optional attachment
 */
public void sendBulkNewsletter(List<String> emails, String subject, String message, MultipartFile attachment) {
    if (emails == null || emails.isEmpty()) {
        System.out.println("[DEBUG] No emails provided for bulk send");
        return;
    }

    int success = 0;
    for (String email : emails) {
        try {
            emailService.sendEmailWithAttachment(email.trim(), subject, message, attachment);
            success++;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send to " + email + ": " + e.getMessage());
        }
    }

    System.out.println("[DEBUG] Bulk newsletter sent: " + success + "/" + emails.size() + " successful");
}
}