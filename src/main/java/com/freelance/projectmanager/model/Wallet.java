package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String userEmail;
    
    private Double balance = 0.0;
}