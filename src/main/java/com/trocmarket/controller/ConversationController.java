package com.trocmarket.controller;

import com.trocmarket.entity.*;
import com.trocmarket.repository.*;
import com.trocmarket.security.JwtUtil;
import com.trocmarket.service.NotificationService; // Import ajouté
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController 
@RequestMapping("/api/conversations") 
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final AnnonceRepository annonceRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService; // Injection ajoutée

    @GetMapping
    public ResponseEntity<?> getConversations(@RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepo.findByEmail(email)
            .map(u -> ResponseEntity.ok(convRepo.findByUserId(u.getId())))
            .orElse(ResponseEntity.status(401).build());
    }

@PostMapping
public ResponseEntity<?> getOrCreate(@RequestBody ConvRequest req, @RequestHeader("Authorization") String auth) {
    String subject = jwtUtil.extractEmail(auth.substring(7));
    var initiateurOpt = userRepo.findByEmail(subject).or(() -> userRepo.findByTelephone(subject));

    return initiateurOpt.<ResponseEntity<?>>map(initiateur -> {
        // 1. Si on a une annonceId, on garde ton ancienne logique intacte
        if (req.getAnnonceId() != null) {
            return annonceRepo.findById(req.getAnnonceId()).map(annonce -> {
                var existing = convRepo.findByAnnonceAndUsers(req.getAnnonceId(), initiateur.getId(), req.getDestinataire());
                if (existing.isPresent()) return ResponseEntity.ok(existing.get());

                return ResponseEntity.ok(convRepo.save(Conversation.builder()
                    .annonce(annonce).initiateur(initiateur).destinataire(userRepo.findById(req.getDestinataire()).get()).build()));
            }).orElseGet(() -> ResponseEntity.notFound().build());
        } 
        
        // 2. NOUVEAU : Si on vient du profil (pas d'annonceId)
        var existingSansAnnonce = convRepo.findByInitiateurIdAndDestinataireId(initiateur.getId(), req.getDestinataire());
        if (existingSansAnnonce.isPresent()) return ResponseEntity.ok(existingSansAnnonce.get());

        return userRepo.findById(req.getDestinataire()).map(dest -> {
            Conversation c = Conversation.builder()
                .initiateur(initiateur)
                .destinataire(dest)
                .build();
            return ResponseEntity.ok(convRepo.save(c));
        }).orElseGet(() -> ResponseEntity.badRequest().build());

    }).orElseGet(() -> ResponseEntity.status(401).build());
}

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id,
                                         @RequestHeader("Authorization") String auth) {
        jwtUtil.extractEmail(auth.substring(7));
        return ResponseEntity.ok(msgRepo.findByConversationIdOrderByCreatedAtAsc(id));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long id,
                                         @RequestBody MsgRequest req,
                                         @RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        
        return userRepo.findByEmail(email).map(auteur ->
            convRepo.findById(id).map(conv -> {
                // 1. Création du message
                Message m = Message.builder()
                    .conversation(conv)
                    .auteur(auteur)
                    .contenu(req.getContenu())
                    .build();

                // 2. Mise à jour de la conversation
                conv.setUpdatedAt(LocalDateTime.now());
                conv.setDernierMessage(req.getContenu().length() > 50
                    ? req.getContenu().substring(0, 50) + "..."
                    : req.getContenu());
                convRepo.save(conv);
                
                Message savedMsg = msgRepo.save(m);

                // 3. --- LOGIQUE DE NOTIFICATION PUSH ---
                // On identifie le destinataire (celui qui n'est pas l'auteur)
                // 3. --- LOGIQUE DE NOTIFICATION PUSH ---
User destinataire = conv.getInitiateur().getId().equals(auteur.getId()) 
                    ? conv.getDestinataire() 
                    : conv.getInitiateur();

notificationService.creer(
    destinataire,
    "MESSAGE",
    "Nouveau message de " + auteur.getNom(),
    req.getContenu(), 
    conv.getId(),        // refId (pour ouvrir la conversation)
    conv.getAnnonce().getId() // <--- AJOUT DU 6ème ARGUMENT (annonceId)
);

                return ResponseEntity.ok(savedMsg);
            }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.status(401).build());
    }

    @Data static class ConvRequest {
        private Long annonceId;
        private Long destinataire;
        private Long destinataireId; // alias mobile
        public Long getDestinataire() {
            return destinataire != null ? destinataire : destinataireId;
        }
    }
    @Data static class MsgRequest  { private String contenu; }
}