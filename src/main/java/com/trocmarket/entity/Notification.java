package com.trocmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User destinataire;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false)
    private String corps;

    // TYPE: PROPOSITION, MESSAGE, TRANSACTION, ANNONCE, SYSTEME
    @Column(nullable = false)
    private String type;

    // Id de la ressource liée (annonceId, convId, txId...)
    // Dans Notification.java
    private Long refId; // ID de la personne (ex: Luciano)

    // AJOUTE CETTE LIGNE
    private Long annonceId; // ID de l'objet (ex: Le Pull)

    @Builder.Default
    private Boolean lue = false;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}