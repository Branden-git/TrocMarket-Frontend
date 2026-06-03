package com.trocmarket.controller;

import com.trocmarket.repository.AnnonceRepository;
import com.trocmarket.repository.UserRepository;
import com.trocmarket.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController @RequestMapping("/api/users") @RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AnnonceRepository annonceRepository;

    // ── GET /api/users/me ──────────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user -> {
            Map<String, Object> response = new HashMap<>();
            response.put("id",             user.getId());
            response.put("nom",            user.getNom());
            response.put("email",          user.getEmail());
            response.put("username",       user.getUsername());
            response.put("telephone",      user.getTelephone());
            response.put("ville",          user.getVille());
            response.put("quartier",       user.getQuartier());
            response.put("bio",            user.getBio());
            response.put("photoUrl",       user.getPhotoUrl());
            response.put("scoreReputation",user.getScoreReputation());
            response.put("nombreEchanges", user.getNombreEchanges());
            response.put("visibilite",     user.getVisibilite());
            response.put("showLocation",   user.getShowLocation());
            response.put("showTelephone",  user.getShowTelephone());
            response.put("showAvis",       user.getShowAvis());
            response.put("notifMessages",  user.getNotifMessages());
            response.put("notifTroc",      user.getNotifTroc());
            response.put("notifPaiement",  user.getNotifPaiement());
            response.put("notifZone",      user.getNotifZone());
            response.put("notifAvis",      user.getNotifAvis());
            response.put("notifPromo",     user.getNotifPromo());
            response.put("nombreAnnonces", annonceRepository.countByProprietaireId(user.getId()));
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── GET /api/users/{id} ────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfil(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ── PUT /api/users/me ──────────────────────────────────────────────────────
    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestHeader("Authorization") String auth,
                                      @RequestBody UpdateRequest req) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user -> {
            if (req.getNom()       != null) user.setNom(req.getNom());
            if (req.getUsername()  != null) user.setUsername(req.getUsername());
            if (req.getTelephone() != null) user.setTelephone(req.getTelephone());
            if (req.getVille()     != null) user.setVille(req.getVille());
            if (req.getQuartier()  != null) user.setQuartier(req.getQuartier());
            if (req.getBio()       != null) user.setBio(req.getBio());
            if (req.getPhotoUrl()  != null) user.setPhotoUrl(req.getPhotoUrl());

            Map<String, Object> response = new HashMap<>();
            var saved = userRepository.save(user);
            response.put("id",             saved.getId());
            response.put("nom",            saved.getNom());
            response.put("email",          saved.getEmail());
            response.put("username",       saved.getUsername());
            response.put("telephone",      saved.getTelephone());
            response.put("ville",          saved.getVille());
            response.put("quartier",       saved.getQuartier());
            response.put("bio",            saved.getBio());
            response.put("photoUrl",       saved.getPhotoUrl());
            response.put("scoreReputation",saved.getScoreReputation());
            response.put("nombreEchanges", saved.getNombreEchanges());
            response.put("visibilite",     saved.getVisibilite());
            response.put("showLocation",   saved.getShowLocation());
            response.put("showTelephone",  saved.getShowTelephone());
            response.put("showAvis",       saved.getShowAvis());
            response.put("notifMessages",  saved.getNotifMessages());
            response.put("notifTroc",      saved.getNotifTroc());
            response.put("notifPaiement",  saved.getNotifPaiement());
            response.put("notifZone",      saved.getNotifZone());
            response.put("notifAvis",      saved.getNotifAvis());
            response.put("notifPromo",     saved.getNotifPromo());
            response.put("nombreAnnonces", annonceRepository.countByProprietaireId(saved.getId()));
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── PUT /api/users/me/visibilite ───────────────────────────────────────────
    @PutMapping("/me/visibilite")
    public ResponseEntity<?> updateVisibilite(@RequestHeader("Authorization") String auth,
                                              @RequestBody VisibiliteRequest req) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user -> {
            if (req.getVisibilite()    != null) user.setVisibilite(req.getVisibilite());
            if (req.getShowLocation()  != null) user.setShowLocation(req.getShowLocation());
            if (req.getShowTelephone() != null) user.setShowTelephone(req.getShowTelephone());
            if (req.getShowAvis()      != null) user.setShowAvis(req.getShowAvis());
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── PUT /api/users/me/notifications ───────────────────────────────────────
    @PutMapping("/me/notifications")
    public ResponseEntity<?> updateNotifications(@RequestHeader("Authorization") String auth,
                                                 @RequestBody NotifPrefsRequest req) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user -> {
            if (req.getNotifMessages() != null) user.setNotifMessages(req.getNotifMessages());
            if (req.getNotifTroc()     != null) user.setNotifTroc(req.getNotifTroc());
            if (req.getNotifPaiement() != null) user.setNotifPaiement(req.getNotifPaiement());
            if (req.getNotifZone()     != null) user.setNotifZone(req.getNotifZone());
            if (req.getNotifAvis()     != null) user.setNotifAvis(req.getNotifAvis());
            if (req.getNotifPromo()    != null) user.setNotifPromo(req.getNotifPromo());
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── DELETE /api/users/me ───────────────────────────────────────────────────
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(@RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user -> {
            userRepository.delete(user);
            return ResponseEntity.ok().body("Compte supprimé");
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── POST /api/users/push-token ─────────────────────────────────────────────
    @PostMapping("/push-token")
    public ResponseEntity<?> savePushToken(
        @RequestHeader("Authorization") String auth,
        @RequestBody PushTokenRequest req) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user -> {
            user.setPushToken(req.getToken());
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────
    @Data static class PushTokenRequest { private String token; }

    @Data
    static class UpdateRequest {
        private String nom, username, telephone, ville, quartier, bio, photoUrl;
    }

    @Data
    static class VisibiliteRequest {
        private String  visibilite;
        private Boolean showLocation;
        private Boolean showTelephone;
        private Boolean showAvis;
    }

    @Data
    static class NotifPrefsRequest {
        private Boolean notifMessages;
        private Boolean notifTroc;
        private Boolean notifPaiement;
        private Boolean notifZone;
        private Boolean notifAvis;
        private Boolean notifPromo;
    }
}