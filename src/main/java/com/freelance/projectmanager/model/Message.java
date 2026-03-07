package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderEmail;
    private String receiverEmail;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String type; // TEXT, IMAGE, CALL_REQUEST (Updated logic)

    @Lob 
    @Column(columnDefinition = "LONGTEXT") 
    private String fileUrl; 

    private boolean isIllegal; 
    private LocalDateTime timestamp;

    public Message() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public boolean isIsIllegal() { return isIllegal; }
    public void setIsIllegal(boolean illegal) { isIllegal = illegal; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}