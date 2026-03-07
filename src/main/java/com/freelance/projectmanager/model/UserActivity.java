package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String userRole;
    private String activityType; // Example: 'CLICK', 'ABANDONED', 'EXIT'
    
    @Column(length = 500)
    private String description;  // Example: 'Started post job but back'
    
    private String pageUrl;
    
    private LocalDateTime timestamp;

    // Default Constructor
    public UserActivity() {}

    // Constructor for easy logging
    public UserActivity(String userEmail, String userRole, String activityType, String description, String pageUrl) {
        this.userEmail = userEmail;
        this.userRole = userRole;
        this.activityType = activityType;
        this.description = description;
        this.pageUrl = pageUrl;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}