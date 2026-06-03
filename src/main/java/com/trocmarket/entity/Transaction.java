package com.trocmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposition_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private Proposition proposition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiateur_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password"})
    private User initiateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepteur_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password"})
    private User recepteur;

    private String codeConfirmation;
    private Boolean confirmeParInitiateur = false;
    private Boolean confirmeParRecepteur  = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutTransaction statut = StatutTransaction.EN_ATTENTE;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime confirmedAt;

    public enum StatutTransaction { EN_ATTENTE, CONFIRMEE, ANNULEE }
}