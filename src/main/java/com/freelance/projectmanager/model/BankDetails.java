package com.freelance.projectmanager.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class BankDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail; 
    private String upiId;
    private String accountNumber;
    private String ifscCode;
    private String accountHolderName;
}