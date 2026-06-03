package com.trocmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","user"})
    private Wallet wallet;

    // CREDIT ou DEBIT
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Double montant;

    // "Vente Samsung S21", "Commission Troc", "Recharge MTN", etc.
    @Column(nullable = false)
    private String libelle;

    // RECHARGE, RETRAIT, COMMISSION, VENTE, TRANSFERT_ENTRANT, TRANSFERT_SORTANT
    @Column(nullable = false)
    private String categorie;

    @Builder.Default
    private String statut = "OK";

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}