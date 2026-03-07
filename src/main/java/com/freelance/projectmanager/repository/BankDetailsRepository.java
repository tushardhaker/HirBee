package com.freelance.projectmanager.repository;

import com.freelance.projectmanager.model.BankDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankDetailsRepository extends JpaRepository<BankDetails, Long> {
    Optional<BankDetails> findByUserEmail(String userEmail);
}