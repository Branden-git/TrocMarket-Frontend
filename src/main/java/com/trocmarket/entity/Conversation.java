package com.trocmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;  

@Entity @Table(name = "conversations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annonce_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","photosUrls","description","proprietaire"})
    private Annonce annonce;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiateur_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password"})
    private User initiateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password"})
    private User destinataire;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
    private String dernierMessage;
}   