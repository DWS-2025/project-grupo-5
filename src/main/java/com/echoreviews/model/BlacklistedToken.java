package com.echoreviews.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken {
    
    @Id
    private String tokenId;
    
    @Column(nullable = false)
    private Date expirationDate;
    
    @Column(nullable = false)
    private Date createdAt;
    
    private String username;
} 