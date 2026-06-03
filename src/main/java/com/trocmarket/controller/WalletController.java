package com.trocmarket.controller;

import com.trocmarket.entity.Wallet;
import com.trocmarket.entity.WalletTransaction;
import com.trocmarket.repository.UserRepository;
import com.trocmarket.repository.WalletRepository;
import com.trocmarket.repository.WalletTransactionRepository;
import com.trocmarket.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletRepository walletRepo;
    private final WalletTransactionRepository txRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    // ── Utilitaire : récupère ou crée le wallet ──
    private Wallet getOrCreate(Long userId, com.trocmarket.entity.User user) {
        return walletRepo.findByUserId(userId).orElseGet(() -> {
            Wallet w = Wallet.builder().user(user).build();
            return walletRepo.save(w);
        });
    }

    // ── GET /api/wallet ── Données complètes du portefeuille ──
    @GetMapping
    public ResponseEntity<?> get(@RequestHeader("Authorization") String auth) {
        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepo.findByEmail(email).map(u -> {
            Wallet w = getOrCreate(u.getId(), u);
            LocalDateTime il30j = LocalDateTime.now().minusDays(30);

            double recu30j        = txRepo.sumCreditAfter(w.getId(), il30j);
            double depense30j     = txRepo.sumDebitAfter(w.getId(), il30j);
            double commissions30j = txRepo.sumCommissionsAfter(w.getId(), il30j);
            long   nbTx30j        = txRepo.countByWalletIdAndCreatedAtAfter(w.getId(), il30j);
            List<WalletTransaction> transactions =
                txRepo.findByWalletIdOrderByCreatedAtDesc(w.getId());

            // Calcul progression fidélité (objectif : 20 transactions pour -1%)
            int txTotal = w.getNombreTransactions();
            int txPourRemise = 20 - (txTotal % 20);
            double progressionFidelite = ((txTotal % 20) / 20.0) * 100;

            Map<String, Object> response = new HashMap<>();
            response.put("solde",               w.getSolde());
            response.put("trocCoins",            w.getTrocCoins());
            response.put("premium",              w.getPremium());
            response.put("tauxCommission",       w.getTauxCommission());
            response.put("nombreTransactions",   txTotal);
            response.put("recu30j",              recu30j);
            response.put("depense30j",           -depense30j); // négatif pour affichage
            response.put("commissions30j",       commissions30j);
            response.put("nbTx30j",              nbTx30j);
            response.put("progressionFidelite",  progressionFidelite);
            response.put("txPourRemise",         txPourRemise);
            response.put("transactions",         transactions);

            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── POST /api/wallet/recharge ──
    @PostMapping("/recharge")
    public ResponseEntity<?> recharge(@RequestBody WalletOp req,
                                      @RequestHeader("Authorization") String auth) {
        if (req.getMontant() == null || req.getMontant() <= 0)
            return ResponseEntity.badRequest().body("Montant invalide");

        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepo.findByEmail(email).map(u -> {
            Wallet w = getOrCreate(u.getId(), u);
            w.setSolde(w.getSolde() + req.getMontant());
            // TrocCoins : 1 par 100 FCFA
            w.setTrocCoins(w.getTrocCoins() + (int)(req.getMontant() / 100));
            walletRepo.save(w);

            // Enregistre la transaction
            txRepo.save(WalletTransaction.builder()
                .wallet(w)
                .type("CREDIT")
                .montant(req.getMontant())
                .libelle("Recharge " + (req.getOperateur() != null ? req.getOperateur().toUpperCase() : ""))
                .categorie("RECHARGE")
                .build());

            return ResponseEntity.ok(Map.of(
                "solde", w.getSolde(),
                "message", "Recharge effectuée avec succès"
            ));
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── POST /api/wallet/retrait ──
    @PostMapping("/retrait")
    public ResponseEntity<?> retrait(@RequestBody WalletOp req,
                                     @RequestHeader("Authorization") String auth) {
        if (req.getMontant() == null || req.getMontant() < 1000)
            return ResponseEntity.badRequest().body("Montant minimum : 1 000 FCFA");

        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepo.findByEmail(email).map(u -> {
            Wallet w = getOrCreate(u.getId(), u);

            // Frais 1% min 100 FCFA
            double frais  = Math.max(100, req.getMontant() * 0.01);
            double total  = req.getMontant() + frais;

            if (w.getSolde() < total)
                return ResponseEntity.badRequest().<Object>body(
                    "Solde insuffisant (montant + frais " + (int)frais + " FCFA = " + (int)total + " FCFA)"
                );

            w.setSolde(w.getSolde() - total);
            walletRepo.save(w);

            txRepo.save(WalletTransaction.builder()
                .wallet(w)
                .type("DEBIT")
                .montant(total)
                .libelle("Retrait " + (req.getOperateur() != null ? req.getOperateur().toUpperCase() : ""))
                .categorie("RETRAIT")
                .build());

            return ResponseEntity.ok(Map.of(
                "solde", w.getSolde(),
                "frais", frais,
                "message", "Retrait initié avec succès"
            ));
        }).orElse(ResponseEntity.status(401).build());
    }

    // ── POST /api/wallet/transfert ──
    @PostMapping("/transfert")
    public ResponseEntity<?> transfert(@RequestBody TransfertOp req,
                                       @RequestHeader("Authorization") String auth) {
        if (req.getMontant() == null || req.getMontant() <= 0)
            return ResponseEntity.badRequest().body("Montant invalide");

        String email = jwtUtil.extractEmail(auth.substring(7));
        return userRepo.findByEmail(email).map(src -> {
            Wallet wSrc = getOrCreate(src.getId(), src);
            if (wSrc.getSolde() < req.getMontant())
                return ResponseEntity.badRequest().<Object>body("Solde insuffisant");

            // Cherche par email ou téléphone
            var destOpt = userRepo.findByEmail(req.getDestinataire());
            if (destOpt.isEmpty())
                destOpt = userRepo.findByTelephone(req.getDestinataire());
            if (destOpt.isEmpty())
                return ResponseEntity.badRequest().<Object>body("Destinataire introuvable");

            var dest  = destOpt.get();
            Wallet wDest = getOrCreate(dest.getId(), dest);

            wSrc.setSolde(wSrc.getSolde() - req.getMontant());
            wDest.setSolde(wDest.getSolde() + req.getMontant());
            walletRepo.save(wSrc);
            walletRepo.save(wDest);

            String motif = req.getMotif() != null ? req.getMotif() : "Transfert";

            txRepo.save(WalletTransaction.builder()
                .wallet(wSrc).type("DEBIT").montant(req.getMontant())
                .libelle("Envoi à " + dest.getNom() + " · " + motif)
                .categorie("TRANSFERT_SORTANT").build());

            txRepo.save(WalletTransaction.builder()
                .wallet(wDest).type("CREDIT").montant(req.getMontant())
                .libelle("Reçu de " + src.getNom() + " · " + motif)
                .categorie("TRANSFERT_ENTRANT").build());

            return ResponseEntity.ok(Map.of(
                "solde", wSrc.getSolde(),
                "message", req.getMontant().intValue() + " FCFA envoyés à " + dest.getNom()
            ));
        }).orElse(ResponseEntity.status(401).build());
    }

    @Data static class WalletOp {
        private Double montant;
        private String operateur;
        private String telephone;
    }

    @Data static class TransfertOp {
        private String destinataire;
        private Double montant;
        private String motif;
    }
}