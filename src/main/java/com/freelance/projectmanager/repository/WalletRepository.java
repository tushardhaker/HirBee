package com.freelance.projectmanager.repository;

import com.freelance.projectmanager.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserEmail(String userEmail);
}