package com.trocmarket.controller;

import com.trocmarket.dto.PropositionRequest;
import com.trocmarket.entity.Proposition;
import com.trocmarket.entity.Proposition.StatutProposition;
import com.trocmarket.entity.User;
import com.trocmarket.repository.AnnonceRepository;
import com.trocmarket.repository.PropositionRepository;
import com.trocmarket.repository.UserRepository;
import com.trocmarket.security.JwtUtil;
import com.trocmarket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/propositions")
@RequiredArgsConstructor
public class PropositionController {

    private final PropositionRepository propositionRepository;
    private final AnnonceRepository annonceRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

    @PostMapping
public ResponseEntity<?> create(@RequestBody PropositionRequest req, @RequestHeader("Authorization") String auth) {
    String token = auth.substring(7);
    String identifier = jwtUtil.extractEmail(token);

    return userRepository.findByEmail(identifier)
            .or(() -> userRepository.findByTelephone(identifier))
            .<ResponseEntity<?>>map(demandeur -> {
                return annonceRepository.findById(req.getAnnonceId()).map(annonce -> {
                    
                    if (annonce.getProprietaire().getId().equals(demandeur.getId())) {
                        return ResponseEntity.badRequest().body("Tu ne peux pas proposer sur ta propre annonce");
                    }

                    Proposition p = Proposition.builder()
                            .annonce(annonce)
                            .demandeur(demandeur)
                            .objetPropose(req.getObjetPropose())
                            .statut(StatutProposition.EN_ATTENTE)
                            .build();

                    Proposition saved = propositionRepository.save(p);

                    // Notification avec le nouvel argument annonceId
                    try {
                        notificationService.creer(
                            annonce.getProprietaire(),
                            "PROPOSITION",
                            "Nouveau troc !",
                            demandeur.getNom() + " propose un échange",
                            demandeur.getId(),
                            annonce.getId() // L'ID de l'annonce pour le clic
                        );
                    } catch (Exception e) {
                        System.err.println("Erreur notification : " + e.getMessage());
                    }

                    return ResponseEntity.ok(saved);
                }).orElseGet(() -> ResponseEntity.status(404).body("Annonce non trouvée"));
            }).orElseGet(() -> ResponseEntity.status(401).build());
}

    @PutMapping("/{id}/statut")
    public ResponseEntity<?> updateStatut(@PathVariable Long id,
                                          @RequestParam StatutProposition statut,
                                          @RequestHeader("Authorization") String auth) {
        
        String token = auth.substring(7);
        String identifier = jwtUtil.extractEmail(token);

        return propositionRepository.findById(id).map(p -> {
            String ownerEmail = p.getAnnonce().getProprietaire().getEmail();
            String ownerPhone = p.getAnnonce().getProprietaire().getTelephone();

            if (!identifier.equals(ownerEmail) && !identifier.equals(ownerPhone)) {
                return ResponseEntity.status(403).build();
            }
            
            p.setStatut(statut);
            Proposition updated = propositionRepository.save(p);

            // Préparation de la notification de réponse
            String typeNotif = (statut == StatutProposition.ACCEPTEE) ? "TRANSACTION" : "PROPOSITION";
            String titreNotif = (statut == StatutProposition.ACCEPTEE) ? "Proposition acceptée ! ✅" : "Proposition refusée ❌";
            String msgNotif = (statut == StatutProposition.ACCEPTEE) ? 
                "Ton échange pour \"" + p.getAnnonce().getTitre() + "\" a été accepté !" :
                "Désolé, ton offre pour \"" + p.getAnnonce().getTitre() + "\" n'a pas été retenue.";

            try {
                // refId = Propriétaire (pour que le demandeur puisse lui écrire), annonceId = contexte
                notificationService.creer(
                    p.getDemandeur(), 
                    typeNotif, 
                    titreNotif, 
                    msgNotif, 
                    p.getAnnonce().getProprietaire().getId(),
                    p.getAnnonce().getId() // <--- AJOUTE CET ARGUMENT ICI
                );
            } catch (Exception e) {
                System.err.println("Erreur notification : " + e.getMessage());
            }

            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/envoyees")
    public ResponseEntity<?> envoyees(@RequestHeader("Authorization") String auth) {
        String identifier = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByTelephone(identifier))
                .map(user -> ResponseEntity.ok(propositionRepository.findByDemandeurId(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/recues")
    public ResponseEntity<?> recues(@RequestHeader("Authorization") String auth) {
        String identifier = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByTelephone(identifier))
                .map(user -> ResponseEntity.ok(propositionRepository.findByAnnonceProprietaireId(user.getId())))
                .orElse(ResponseEntity.status(401).build());
    }
}