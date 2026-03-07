package com.freelance.projectmanager.controller;

import com.freelance.projectmanager.model.Message;
import com.freelance.projectmanager.repository.MessageRepository; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
// @CrossOrigin(origins = { "http://localhost:5500", "https://hir-bee-3nwb.vercel.app" }, allowCredentials = "true", allowedHeaders = "*")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @PostMapping("/send")
    public ResponseEntity<Message> sendMessage(@RequestBody Message message) {
        try {
            message.setTimestamp(LocalDateTime.now());
            
            // LOGIC UPDATED:
            // 1. Agar type pehle se 'CALL_REQUEST' hai, toh use mat chhedo.
            // 2. Agar fileUrl hai toh 'IMAGE' set karo.
            // 3. Baaki sab 'TEXT'.
            
            if ("CALL_REQUEST".equals(message.getType())) {
                // Keep as is, backend shouldn't overwrite frontend's call request type
            } else if (message.getFileUrl() != null && message.getFileUrl().startsWith("data:image")) {
                message.setType("IMAGE");
                if (message.getMessage() == null || message.getMessage().isEmpty()) {
                    message.setMessage("Sent a photo");
                }
            } else {
                message.setType("TEXT");
            }
            
            Message savedMessage = messageRepository.save(message);
            return ResponseEntity.ok(savedMessage);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<Message>> getChatHistory(@RequestParam String me, @RequestParam String with) {
        List<Message> history = messageRepository.findChatHistory(me, with);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/partners")
    public ResponseEntity<List<String>> getChatPartners(@RequestParam String email) {
        List<String> partners = messageRepository.findChatPartners(email);
        return ResponseEntity.ok(partners);
    }

    @GetMapping("/flagged")
    public ResponseEntity<List<Message>> getFlaggedMessages() {
        return ResponseEntity.ok(messageRepository.findByIsIllegalTrue());
    }
}