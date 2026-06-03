package com.trocmarket.controller;

import com.trocmarket.entity.Notation;
import com.trocmarket.entity.Proposition;
import com.trocmarket.repository.NotationRepository;
import com.trocmarket.repository.PropositionRepository;
import com.trocmarket.repository.UserRepository;
import com.trocmarket.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/notations") @RequiredArgsConstructor
public class NotationController {

    private final NotationRepository notationRepository;
    private final PropositionRepository propositionRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

@PostMapping
public ResponseEntity<?> noter(@RequestBody NoteRequest req,
                                @RequestHeader("Authorization") String auth) {
    String email = jwtUtil.extractEmail(auth.substring(7));
    
    return userRepository.findByEmail(email).<ResponseEntity<?>>map(noteur ->
        propositionRepository.findById(req.getTransactionId()).<ResponseEntity<?>>map(prop -> {
            // Trouve l'autre personne (la personne notée)
            Long noteUserId = prop.getDemandeur().getId().equals(noteur.getId())
                ? prop.getAnnonce().getProprietaire().getId()
                : prop.getDemandeur().getId();

            return userRepository.findById(noteUserId).map(noteUser -> {
                Notation n = Notation.builder()
                    .noteur(noteur)
                    .noteUser(noteUser)
                    .proposition(prop)
                    .score(req.getNoteGlobale())
                    .commentaire(req.getCommentaire())
                    .build();

                // Mise à jour du score de réputation
                double ancien = noteUser.getScoreReputation() != null ? noteUser.getScoreReputation() : 0;
                int nb = noteUser.getNombreEchanges() != null ? noteUser.getNombreEchanges() : 0;
                double nouveau = (ancien * nb + req.getNoteGlobale()) / (nb + 1.0);
                
                noteUser.setScoreReputation(Math.round(nouveau * 10.0) / 10.0);
                noteUser.setNombreEchanges(nb + 1);
                userRepository.save(noteUser);

                return ResponseEntity.ok(notationRepository.save(n));
            }).orElseGet(() -> ResponseEntity.badRequest().build());
        }).orElseGet(() -> ResponseEntity.notFound().build())
    ).orElseGet(() -> ResponseEntity.status(401).build());
}

    @Data
    static class NoteRequest {
        private Long transactionId;
        private Integer noteGlobale;
        private Integer ponctualite;
        private Integer conformite;
        private Integer communication;
        private Integer fiabilite;
        private String commentaire;
        private Boolean anonyme;
    }
}