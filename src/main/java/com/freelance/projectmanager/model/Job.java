package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
@Data
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; 
    
    // --- NEW FIELD ---
    private String category; 

    private String clientEmail;
    
    @Column(columnDefinition = "TEXT")
    private String details;

    private Double budget;
    private String mode; // ONLINE, OFFLINE, HYBRID
    private String location;

    private String status; // ACTIVE or BANNED

    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}