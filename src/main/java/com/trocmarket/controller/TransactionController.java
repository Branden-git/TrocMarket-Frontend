package com.trocmarket.controller;

import com.trocmarket.entity.Transaction;
import com.trocmarket.repository.*;
import com.trocmarket.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@RestController @RequestMapping("/api/transactions") @RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository txRepo;
    private final PropositionRepository propRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id,
                                    @RequestHeader("Authorization") String auth) {
        jwtUtil.extractEmail(auth.substring(7));
        return txRepo.findById(id).map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Créer une transaction lors de l'acceptation d'une proposition
    @PostMapping("/creer/{propositionId}")
    public ResponseEntity<?> creer(@PathVariable Long propositionId,
                                   @RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepo.findByEmail(email).map(user ->
            propRepo.findById(propositionId).map(prop -> {
                // Vérifie que la proposition est acceptée
                if (prop.getStatut() != com.trocmarket.entity.Proposition.StatutProposition.ACCEPTEE)
                    return ResponseEntity.badRequest().<Object>body("Proposition non acceptée");
                // Vérifie qu'une transaction n'existe pas déjà
                var existing = txRepo.findByPropositionId(propositionId);
                if (existing.isPresent()) return ResponseEntity.ok(existing.get());
                // Génère code 6 chiffres
                String code = String.format("%06d", new Random().nextInt(999999));
                Transaction tx = Transaction.builder()
                    .proposition(prop)
                    .initiateur(prop.getDemandeur())
                    .recepteur(prop.getAnnonce().getProprietaire())
                    .codeConfirmation(code)
                    .build();
                return ResponseEntity.ok(txRepo.save(tx));
            }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/{id}/confirmer")
    public ResponseEntity<?> confirmer(@PathVariable Long id,
                                       @RequestBody ConfirmReq req,
                                       @RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepo.findByEmail(email).map(user ->
            txRepo.findById(id).map(tx -> {
                if (!tx.getCodeConfirmation().equals(req.getCode()))
                    return ResponseEntity.badRequest().<Object>body("Code incorrect");
                boolean isInit = tx.getInitiateur().getId().equals(user.getId());
                if (isInit)  tx.setConfirmeParInitiateur(true);
                else         tx.setConfirmeParRecepteur(true);
                if (Boolean.TRUE.equals(tx.getConfirmeParInitiateur()) &&
                    Boolean.TRUE.equals(tx.getConfirmeParRecepteur())) {
                    tx.setStatut(Transaction.StatutTransaction.CONFIRMEE);
                    tx.setConfirmedAt(LocalDateTime.now());
                    // Met à jour le statut de l'annonce
                    tx.getProposition().getAnnonce().setStatut(com.trocmarket.entity.Annonce.StatutAnnonce.TERMINEE);
                }
                return ResponseEntity.ok(txRepo.save(tx));
            }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.status(401).build());
    }

    @Data static class ConfirmReq { private String code; }
}