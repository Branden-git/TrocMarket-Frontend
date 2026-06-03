package com.trocmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wallets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wallet {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password"})
    private User user;

    @Builder.Default
    private Double solde = 0.0;

    // TrocCoins = 1 TrocCoin par 100 FCFA de transactions
    @Builder.Default
    private Integer trocCoins = 0;

    @Builder.Default
    private Boolean premium = false;

    // Taux de commission actuel (5% par défaut, réduit selon fidélité)
    @Builder.Default
    private Double tauxCommission = 5.0;

    // Nombre total de transactions validées (pour progression fidélité)
    @Builder.Default
    private Integer nombreTransactions = 0;
}