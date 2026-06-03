package com.trocmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "propositions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Proposition {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annonce_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "photosUrls", "description"})
    private Annonce annonce;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    private User demandeur;

    private String objetPropose;
    private String descriptionObjetPropose;
    private Double montantPropose;
    private String message;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private StatutProposition statut = StatutProposition.EN_ATTENTE;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum StatutProposition { EN_ATTENTE, ACCEPTEE, REFUSEE, ANNULEE }
}