package com.trocmarket.controller;

import com.trocmarket.repository.NotificationRepository;
import com.trocmarket.repository.UserRepository;
import com.trocmarket.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // POST /api/notifications/push-token — sauvegarder le token Expo
@PostMapping("/push-token")
public ResponseEntity<?> savePushToken(
        @RequestHeader("Authorization") String auth,
        @RequestBody java.util.Map<String, String> body) {
    String email = jwtUtil.extractEmail(auth.substring(7));
    return userRepository.findByEmail(email).map(user -> {
        user.setPushToken(body.get("pushToken"));
        userRepository.save(user);
        return ResponseEntity.ok(java.util.Map.of("message", "Token sauvegardé"));
    }).orElse(ResponseEntity.status(401).build());
}

    // GET /api/notifications — liste mes notifs
    @GetMapping
    public ResponseEntity<?> getMesNotifications(
            @RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user ->
            ResponseEntity.ok(
                notificationRepository.findByDestinataireIdOrderByCreatedAtDesc(user.getId())
            )
        ).orElse(ResponseEntity.status(401).build());
    }

    // GET /api/notifications/count — nombre de non lues
    @GetMapping("/count")
    public ResponseEntity<?> countNonLues(
            @RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user ->
            ResponseEntity.ok(
                java.util.Map.of("count",
                    notificationRepository.countByDestinataireIdAndLueFalse(user.getId()))
            )
        ).orElse(ResponseEntity.status(401).build());
    }

    // PUT /api/notifications/{id}/lue — marquer une notif lue
    @PutMapping("/{id}/lue")
    public ResponseEntity<?> marquerLue(
            @PathVariable Long id,
            @RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return notificationRepository.findById(id).map(n -> {
            if (!n.getDestinataire().getEmail().equals(email))
                return ResponseEntity.status(403).<Object>build();
            n.setLue(true);
            notificationRepository.save(n);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/notifications/lire-tout — marquer toutes lues
    @PutMapping("/lire-tout")
    public ResponseEntity<?> marquerToutesLues(
            @RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepository.findByEmail(email).map(user -> {
            notificationRepository.marquerToutesLues(user.getId());
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.status(401).build());
    }
}