package com.trocmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = true, unique = true)
    private String telephone;

    // Ajoute cette ligne dans la classe User après photoUrl
    @Column(name = "push_token", length = 500)
    private String pushToken;

    private String ville;
    private String quartier;
    private String photoUrl;
   
    @Builder.Default private Double scoreReputation = 0.0;
    @Builder.Default private Integer nombreEchanges = 0;
    @Builder.Default private Boolean actif = true;

    @Column(nullable = false, updatable = false)
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();

    private String  bio;
private String  username;
private String  visibilite = "public";
private Boolean showLocation  = true;
private Boolean showTelephone = false;
private Boolean showAvis      = true;
private Boolean notifMessages = true;
private Boolean notifTroc     = true;
private Boolean notifPaiement = true;
private Boolean notifZone     = true;
private Boolean notifAvis     = true;
private Boolean notifPromo    = false;
}