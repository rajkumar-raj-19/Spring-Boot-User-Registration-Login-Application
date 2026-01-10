package com.example.demo;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "USERS_LIST")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(name = "user_seq", sequenceName = "USERS_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "FULL_NAME")
    private String fullName;

    private String email;
    
    // This matches your SQL PASSWORD column
    private String password;

    private String provider;

    @Column(name = "PHOTO_URL")
    private String photoUrl;
}