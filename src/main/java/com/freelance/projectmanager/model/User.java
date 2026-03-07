package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String mobile;

    @Column(nullable = false)
    private String password;

    @Lob 
    @Column(name = "profile_image", columnDefinition = "LONGTEXT") 
    private String profileImage;
    
    private String location;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    private Role role;

    // --- AUTOMATIC SUBSCRIPTION FIELDS ---
    private String subscriptionPlan = "FREE"; 

    // Status: NONE, ACTIVE (PENDING hata diya gaya hai)
    private String subscriptionStatus = "NONE"; 

    @Column(nullable = false)
    private boolean isTrending = false;

    private LocalDateTime subscriptionEndDate;

    public enum Role {
        FREELANCER, CLIENT, ADMIN
    }
}