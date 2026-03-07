package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@NoArgsConstructor // Default constructor for JPA
@AllArgsConstructor // Parameterized constructor for your needs
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String userEmail;
    
    private Double balance = 0.0;

    // Optional: Agar sirf email aur balance wala constructor chahiye
    public Wallet(String userEmail, Double balance) {
        this.userEmail = userEmail;
        this.balance = balance;
    }
}