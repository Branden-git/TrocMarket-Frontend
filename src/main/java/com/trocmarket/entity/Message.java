package com.trocmarket.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auteur_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler","password"})
    private User auteur;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TypeMessage type = TypeMessage.TEXT;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TypeMessage { TEXT, PROPOSITION, SYSTEME }
}