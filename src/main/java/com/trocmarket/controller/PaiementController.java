package com.trocmarket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trocmarket.entity.Annonce;
import com.trocmarket.entity.Paiement;
import com.trocmarket.entity.User;
import com.trocmarket.entity.Wallet;
import com.trocmarket.repository.AnnonceRepository;
import com.trocmarket.repository.PaiementRepository;
import com.trocmarket.repository.UserRepository;
import com.trocmarket.repository.WalletRepository;
import com.trocmarket.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
public class PaiementController {

    private static final Logger log = LoggerFactory.getLogger(PaiementController.class);

    private final AnnonceRepository  annonceRepo;
    private final UserRepository     userRepo;
    private final WalletRepository   walletRepo;
    private final PaiementRepository paiementRepo;
    private final JwtUtil            jwtUtil;

    @Value("${monetbil.service.key}")
    private String monetbilServiceKey;

    @Value("${monetbil.api.url}")
    private String monetbilApiUrl;

    @Value("${monetbil.notify.url:https://CONFIGURE-NGROK.ngrok-free.app}")
    private String notifyUrl;

    @PostMapping("/initier")
    public ResponseEntity<?> initier(@RequestBody PaiementRequest req,
                                     @RequestHeader("Authorization") String auth) throws Exception {

        log.info("=== PAIEMENT INITIER ===");
        log.info("annonceId={} montant={} operateur={} telephone={}",
            req.getAnnonceId(), req.getMontant(), req.getOperateur(), req.getTelephone());

        String email = jwtUtil.extractEmail(auth.substring(7));
        User acheteur = userRepo.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Annonce annonce = annonceRepo.findById(req.getAnnonceId())
            .orElseThrow(() -> new RuntimeException("Annonce introuvable"));

        double commission = Math.round(req.getMontant() * 0.05 * 100.0) / 100.0;

        Paiement paiement = Paiement.builder()
            .acheteur(acheteur)
            .annonce(annonce)
            .montant(req.getMontant())
            .commission(commission)
            .operateur(req.getOperateur())
            .telephone(req.getTelephone())
            .build();
        paiement = paiementRepo.save(paiement);
        log.info("Paiement sauvegardé en BD avec id={}", paiement.getId());

        try {
            RestTemplate rt = new RestTemplate();

            String operateurMonetbil = req.getOperateur().equals("mtn")
                ? "CM_MTNMOBILEMONEY" : "CM_ORANGEMONEY";

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("service",     monetbilServiceKey);
            params.add("amount",      String.valueOf(req.getMontant().intValue()));
            params.add("phone",       req.getTelephone());
            params.add("country",     "CM");
            params.add("currency",    "XAF");
            params.add("operator",    operateurMonetbil);
            params.add("item_ref",    "ANNONCE-" + annonce.getId());
            params.add("payment_ref", "TM-" + paiement.getId() + "-" + System.currentTimeMillis());
            params.add("notify_url",  notifyUrl + "/api/paiements/webhook");
            params.add("return_url",  "trocmarket://paiement/" + annonce.getId() + "/succes");
            params.add("first_name",  acheteur.getNom().split(" ")[0]);
            params.add("last_name",   acheteur.getNom().contains(" ")
                ? acheteur.getNom().split(" ", 2)[1] : ".");
            params.add("email",       acheteur.getEmail().contains("@trocmarket.cm")
                ? "noreply@trocmarket.cm" : acheteur.getEmail());

            String url = monetbilApiUrl + "/" + monetbilServiceKey;
            log.info("Appel Monetbil → URL=[{}]", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = rt.postForEntity(url, entity, String.class);

            log.info("Réponse Monetbil HTTP status=[{}]", response.getStatusCode());
            log.info("Réponse Monetbil raw=[{}]", response.getBody());

            String rawBody = response.getBody();
            if (rawBody == null || rawBody.isBlank()) {
                return ResponseEntity.status(502).body(Map.of("error", "Réponse vide de Monetbil"));
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> body = mapper.readValue(rawBody, Map.class);

            // ← FIX : Monetbil renvoie true (boolean), pas "success" (string)
            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                String paymentId  = String.valueOf(body.getOrDefault("payment_id", ""));
                String paymentRef = String.valueOf(body.getOrDefault("payment_ref", "TM-" + paiement.getId()));
                String paymentUrl = String.valueOf(body.getOrDefault("payment_url", ""));

                paiement.setPaymentId(paymentId);
                paiement.setReferenceMonetbil(paymentRef);
                paiementRepo.save(paiement);

                log.info("✅ Paiement initié — paymentId=[{}] ref=[{}] url=[{}]", paymentId, paymentRef, paymentUrl);

                return ResponseEntity.ok(Map.of(
                    "paiementId", paiement.getId(),
                    "paymentId",  paymentId,
                    "reference",  paymentRef,
                    "paymentUrl", paymentUrl,
                    "montant",    req.getMontant(),
                    "commission", commission,
                    "operateur",  req.getOperateur(),
                    "telephone",  req.getTelephone(),
                    "statut",     "EN_ATTENTE",
                    "message",    buildUssdMessage(req.getOperateur(), req.getMontant())
                ));
            } else {
                String errMsg = body != null
                    ? String.valueOf(body.getOrDefault("message", "Erreur Monetbil"))
                    : "Réponse vide";
                log.error("Monetbil erreur : [{}] — body=[{}]", errMsg, body);
                return ResponseEntity.status(502).body(Map.of("error", "Monetbil: " + errMsg));
            }

        } catch (Exception e) {
            log.error("EXCEPTION lors de l'appel Monetbil : {}", e.getMessage(), e);

            String ref = "TM-" + paiement.getId() + "-" + System.currentTimeMillis();
            paiement.setReferenceMonetbil(ref);
            paiementRepo.save(paiement);

            return ResponseEntity.ok(Map.of(
                "paiementId", paiement.getId(),
                "reference",  ref,
                "montant",    req.getMontant(),
                "commission", commission,
                "operateur",  req.getOperateur(),
                "telephone",  req.getTelephone(),
                "statut",     "EN_ATTENTE",
                "fallback",   true,
                "message",    buildUssdMessage(req.getOperateur(), req.getMontant())
            ));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody(required = false) Map<String, Object> body,
                                     @RequestParam(required = false) Map<String, String> params) {
        log.info("=== WEBHOOK MONETBIL reçu === body={} params={}", body, params);

        String paymentRef  = body != null
            ? String.valueOf(body.getOrDefault("payment_ref", ""))
            : params.getOrDefault("payment_ref", "");
        String transStatus = body != null
            ? String.valueOf(body.getOrDefault("status", ""))
            : params.getOrDefault("status", "");

        log.info("paymentRef=[{}] status=[{}]", paymentRef, transStatus);
        if (paymentRef.isBlank()) return ResponseEntity.ok("OK");

        paiementRepo.findByReferenceMonetbil(paymentRef).ifPresent(paiement -> {
            if ("1".equals(transStatus) || "success".equalsIgnoreCase(transStatus)) {
                paiement.setStatut(Paiement.StatutPaiement.SUCCES);
                paiement.setPaidAt(LocalDateTime.now());

                User vendeur = paiement.getAnnonce().getProprietaire();
                Wallet wallet = walletRepo.findByUserId(vendeur.getId()).orElseGet(() ->
                    walletRepo.save(Wallet.builder().user(vendeur).solde(0.0).build())
                );
                double net = paiement.getMontant() - paiement.getCommission();
                wallet.setSolde(wallet.getSolde() + net);
                walletRepo.save(wallet);

                paiement.getAnnonce().setStatut(Annonce.StatutAnnonce.TERMINEE);
                annonceRepo.save(paiement.getAnnonce());
                log.info("✅ Paiement {} SUCCES — vendeur crédité {} FCFA", paiement.getId(), net);

            } else if ("cancelled".equalsIgnoreCase(transStatus) || "0".equals(transStatus)) {
                paiement.setStatut(Paiement.StatutPaiement.ECHEC);
                log.info("❌ Paiement {} ECHEC", paiement.getId());
            }
            paiementRepo.save(paiement);
        });

        return ResponseEntity.ok("OK");
    }

    @GetMapping("/{id}/statut")
    public ResponseEntity<?> statut(@PathVariable Long id,
                                    @RequestHeader("Authorization") String auth) {
        jwtUtil.extractEmail(auth.substring(7));
        return paiementRepo.findById(id)
            .map(p -> ResponseEntity.ok(Map.of(
                "paiementId", p.getId(),
                "statut",     p.getStatut(),
                "montant",    p.getMontant(),
                "reference",  p.getReferenceMonetbil() != null ? p.getReferenceMonetbil() : "",
                "paidAt",     p.getPaidAt() != null ? p.getPaidAt().toString() : ""
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/mes-paiements")
    public ResponseEntity<?> mesPaiements(@RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepo.findByEmail(email)
            .map(u -> ResponseEntity.ok(paiementRepo.findByAcheteurIdOrderByCreatedAtDesc(u.getId())))
            .orElse(ResponseEntity.status(401).build());
    }

    private String buildUssdMessage(String operateur, Double montant) {
        if ("mtn".equalsIgnoreCase(operateur)) {
            return "Sur ton téléphone MTN, compose *126# → Envoyer de l'argent → entre le montant "
                + montant.intValue() + " FCFA et la référence de paiement.";
        } else {
            return "Sur ton téléphone Orange, compose #150*50# → Paiement marchand → entre le montant "
                + montant.intValue() + " FCFA et la référence.";
        }
    }

    @Data static class PaiementRequest {
        private Long   annonceId;
        private Double montant;
        private String operateur;
        private String telephone;
    }
}