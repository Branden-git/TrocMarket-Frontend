package com.trocmarket.controller;

import com.trocmarket.dto.*;
import com.trocmarket.entity.User;
import com.trocmarket.repository.UserRepository;
import com.trocmarket.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController 
@RequestMapping("/api/auth") 
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        // 1. Détermination de l'email final
        String emailSaisi = req.getEmail();
        String telSaisi = req.getTelephone();
        String finalEmail;

        // Si l'utilisateur a saisi un email, on l'utilise
        if (emailSaisi != null && !emailSaisi.isBlank()) {
            if (userRepository.existsByEmail(emailSaisi)) {
                return ResponseEntity.badRequest().body("Email déjà utilisé");
            }
            finalEmail = emailSaisi.toLowerCase().trim();
        } 
        // Sinon, on génère un email technique basé sur le téléphone
        else if (telSaisi != null && !telSaisi.isBlank()) {
            if (userRepository.existsByTelephone(telSaisi)) {
                return ResponseEntity.badRequest().body("Numéro déjà utilisé");
            }
            // On nettoie le numéro (enlève le +) pour créer l'email
            String telClean = telSaisi.replace("+", "").trim();
            finalEmail = telClean + "@trocmarket.cm";
        } 
        else {
            return ResponseEntity.badRequest().body("Email ou téléphone requis");
        }

        // 2. Création de l'utilisateur
        User user = User.builder()
            .nom(req.getNom())
            .email(finalEmail)
            .password(passwordEncoder.encode(req.getPassword()))
            .telephone(telSaisi)
            .ville(req.getVille() != null && !req.getVille().isBlank() ? req.getVille() : "Yaoundé")
            .quartier(req.getQuartier() != null ? req.getQuartier() : "")
            .build();

        userRepository.save(user);

        // 3. Génération du token (utilise toujours l'email car il est maintenant garanti)
        String token = jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getNom(), user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.getIdentifiant() == null || req.getIdentifiant().isBlank())
            return ResponseEntity.badRequest().body("Email ou téléphone requis");
        if (req.getPassword() == null || req.getPassword().isBlank())
            return ResponseEntity.badRequest().body("Mot de passe requis");

        // Cherche par email ou téléphone
        User user = null;
        String identifiant = req.getIdentifiant().trim();

        if (identifiant.contains("@")) {
            user = userRepository.findByEmail(identifiant.toLowerCase()).orElse(null);
        } else {
            // Normalise le numéro pour la recherche
            String tel = identifiant.replaceAll("[\\s\\-]", "");
            if (!tel.startsWith("+")) tel = "+237" + tel;
            user = userRepository.findByTelephone(tel).orElse(null);
        }

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword()))
            return ResponseEntity.status(401).body("Identifiant ou mot de passe incorrect");

        String token = jwtUtil.generateToken(user.getEmail());
        
        return ResponseEntity.ok(new AuthResponse(token, user.getEmail(), user.getNom(), user.getId()));
    }
}