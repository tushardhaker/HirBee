package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposals")
public class Proposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "freelancer_email")
    private String freelancerEmail;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "job_details", columnDefinition = "LONGTEXT")
    private String jobDetails;
    
    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;
    
    @Column(name = "bid_amount")
    private Double bidAmount;

    private String status; // PENDING, ACCEPTED, REJECTED

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Proposal() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getFreelancerEmail() { return freelancerEmail; }
    public void setFreelancerEmail(String freelancerEmail) { this.freelancerEmail = freelancerEmail; }
    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }
    public String getJobDetails() { return jobDetails; }
    public void setJobDetails(String jobDetails) { this.jobDetails = jobDetails; }
    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
    public Double getBidAmount() { return bidAmount; }
    public void setBidAmount(Double bidAmount) { this.bidAmount = bidAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}