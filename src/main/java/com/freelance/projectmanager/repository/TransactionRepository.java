package com.freelance.projectmanager.repository;

import com.freelance.projectmanager.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Ye method user ke saare transactions (Sent and Received) dhoondh nikaalega
    List<Transaction> findBySenderEmailOrReceiverEmail(String senderEmail, String receiverEmail);
}