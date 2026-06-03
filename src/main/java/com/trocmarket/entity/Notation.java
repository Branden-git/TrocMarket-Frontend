package com.trocmarket.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "noteur_id", nullable = false)
    private User noteur;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "note_id", nullable = false)
    private User noteUser;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "proposition_id", nullable = false)
    private Proposition proposition;
    @Column(nullable = false) private Integer score;
    private String commentaire;
    @Column(nullable = false, updatable = false)
    @Builder.Default private LocalDateTime createdAt = LocalDateTime.now();
}
