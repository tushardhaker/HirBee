package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_subscribers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "subscribed_at", nullable = false, updatable = false)
    private LocalDateTime subscribedAt = LocalDateTime.now();

    @Column(length = 50)
    private String source;  // "signup" or "homepage"

    @Column(name = "user_id")
    private Long userId;   // optional - link to registered user

    /**
     * Transient field: Role from linked User (not stored in DB)
     * Will be populated in service layer when fetching list
     */
    @Transient
    private String role;   // e.g. "FREELANCER", "CLIENT", null (guest)

    // Optional: add convenience method for frontend display
    public String getDisplayRole() {
        return role != null ? role : "Guest";
    }
}