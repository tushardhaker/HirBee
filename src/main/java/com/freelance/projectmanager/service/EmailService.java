package com.freelance.projectmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public class EmailService {

    private final RestTemplate restTemplate;

    @Value("${sendgrid.api.key}")
    private String sendgridApiKey;

    @Value("${sendgrid.sender.email:coccrv109@gmail.com}")
    private String senderEmail;

    @Value("${sendgrid.sender.name:HirBee}")
    private String senderName;

    public EmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String generateOTP() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    public void sendOtpEmail(String to, String otp) {
        sendEmail(to.trim(), "Your Verification Code - HirBee",
                "Your OTP is: " + otp + "\n\nThis code is valid for 10 minutes. Do not share with anyone.",
                null);
    }

    public void sendNoticeEmail(String to, String subject, String body) {
        sendEmail(to.trim(), subject, body, null);
    }

    public void sendEmailWithAttachment(String to, String subject, String body, MultipartFile attachment) {
        sendEmail(to.trim(), subject, body, attachment);
    }

    private void sendEmail(String to, String subject, String textContent, MultipartFile attachment) {
        String url = "https://api.sendgrid.com/v3/mail/send";

        Map<String, Object> from = Map.of("email", senderEmail, "name", senderName);

        Map<String, Object> personalization = new HashMap<>();
        personalization.put("to", List.of(Map.of("email", to)));

        Map<String, Object> content = Map.of("type", "text/plain", "value", textContent);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("from", from);
        requestBody.put("personalizations", List.of(personalization));
        requestBody.put("subject", subject);
        requestBody.put("content", List.of(content));

        // Attachment (optional)
        if (attachment != null && !attachment.isEmpty()) {
            try {
                Map<String, Object> attach = new HashMap<>();
                attach.put("content", Base64.getEncoder().encodeToString(attachment.getBytes()));
                attach.put("filename", attachment.getOriginalFilename());
                attach.put("disposition", "attachment");
                attach.put("type", attachment.getContentType() != null ? attachment.getContentType() : "application/octet-stream");
                requestBody.put("attachments", List.of(attach));
            } catch (Exception e) {
                throw new RuntimeException("Attachment failed: " + e.getMessage());
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + sendgridApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            String error = response.getBody() != null ? response.getBody() : "No body";
            throw new RuntimeException("SendGrid error - Status: " + response.getStatusCode() + ", " + error);
        }
    }
}