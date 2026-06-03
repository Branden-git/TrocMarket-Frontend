package com.trocmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "paiements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Paiement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acheteur_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password"})
    private User acheteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annonce_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private Annonce annonce;

    private String referenceMonetbil;   // payment_ref retourné par Monetbil
    private String paymentId;           // payment_id Monetbil
    private Double montant;
    private Double commission;          // 5%
    private String operateur;           // mtn ou orange
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutPaiement statut = StatutPaiement.EN_ATTENTE;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime paidAt;

    public enum StatutPaiement { EN_ATTENTE, SUCCES, ECHEC, ANNULE }
}