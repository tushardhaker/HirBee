package com.freelance.projectmanager.repository;

import com.freelance.projectmanager.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiverEmailOrderByTimestampDesc(String receiverEmail);

    @Query("SELECT m FROM Message m WHERE (m.senderEmail = :me AND m.receiverEmail = :partner) " +
           "OR (m.senderEmail = :partner AND m.receiverEmail = :me) ORDER BY m.timestamp ASC")
    List<Message> findChatHistory(@Param("me") String me, @Param("partner") String partner);

    @Query(value = "SELECT DISTINCT partner_email FROM (" +
           "SELECT receiver_email AS partner_email FROM messages WHERE sender_email = :email " +
           "UNION " +
           "SELECT sender_email AS partner_email FROM messages WHERE receiver_email = :email" +
           ") as chat_partners", nativeQuery = true)
    List<String> findChatPartners(@Param("email") String email);

    List<Message> findByIsIllegalTrue();
}