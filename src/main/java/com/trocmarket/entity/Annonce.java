package com.trocmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "annonces")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Annonce {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAnnonce type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutAnnonce statut = StatutAnnonce.ACTIVE;

    private Double prix;
    private Double valeurEstimee; // Important pour le troc équitable
    private String categorie;
    private String etat; 
    private String ville;
    private String quartier;

    // --- Géolocalisation ---
    private Double latitude;
    private Double longitude;

    // --- Statistiques et Flags ---
    @Builder.Default private Integer nombreVues     = 0;
    @Builder.Default private Integer nombreFavoris  = 0;
    @Builder.Default private Boolean urgent         = false;
    @Builder.Default private Boolean negociable     = false;

    // --- Collections ---
    @ElementCollection
    @CollectionTable(name = "annonce_recherche", joinColumns = @JoinColumn(name = "annonce_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> rechercheContre = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "annonce_photos", joinColumns = @JoinColumn(name = "annonce_id"))
    @Column(name = "photo_url", columnDefinition = "LONGTEXT") // Supporte le Base64
    @Builder.Default
    private List<String> photosUrls = new ArrayList<>();

    // --- Relations ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "annonce_favoris",
        joinColumns = @JoinColumn(name = "annonce_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password","annonces", "notifications", "propositions"})
    private List<User> favorisUsers = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "annonces", "notifications", "propositions"})
    private User proprietaire;

    // --- Dates ---
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    // --- Enums ---
    public enum TypeAnnonce   { TROC, VENTE, DON }
    public enum StatutAnnonce { ACTIVE, EN_COURS, TERMINEE, ANNULEE, PAUSEE }

    // --- Lifecycle Hook ---
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}