package com.trocmarket.controller;

import com.trocmarket.dto.AnnonceRequest;
import com.trocmarket.entity.Annonce;
import com.trocmarket.entity.Annonce.TypeAnnonce;
import com.trocmarket.entity.Annonce.StatutAnnonce;
import com.trocmarket.entity.User;
import com.trocmarket.repository.AnnonceRepository;
import com.trocmarket.repository.UserRepository;
import com.trocmarket.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.ArrayList;

@RestController 
@RequestMapping("/api/annonces") 
@RequiredArgsConstructor
public class AnnonceController {

    private final AnnonceRepository annonceRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;


private double[] geocode(String ville, String quartier) {
    try {
        String query = (quartier != null && !quartier.isBlank() ? quartier + ", " : "") + ville + ", Cameroun";
        String url = UriComponentsBuilder
            .fromHttpUrl("https://nominatim.openstreetmap.org/search")
            .queryParam("q", query)
            .queryParam("format", "json")
            .queryParam("limit", "1")
            .toUriString();

        RestTemplate rt = new RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", "TrocMarket/1.0");
        var entity = new org.springframework.http.HttpEntity<>(headers);
        var response = rt.exchange(url, org.springframework.http.HttpMethod.GET, entity, java.util.List.class);

        var results = response.getBody();
        if (results != null && !results.isEmpty()) {
            var first = (java.util.Map<?, ?>) results.get(0);
            double lat = Double.parseDouble(first.get("lat").toString());
            double lon = Double.parseDouble(first.get("lon").toString());
            return new double[]{lat, lon};
        }
    } catch (Exception e) {
        // silencieux — coordonnées resteront null
    }
    return null;
}

    // ── Liste / Recherche ────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TypeAnnonce type,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String quartier,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String etat,
            @RequestParam(required = false) Boolean urgent,
            @RequestParam(required = false) Boolean negociable,
            @RequestParam(required = false) Boolean avecPhoto,
            @RequestParam(required = false) Double prixMin,
            @RequestParam(required = false) Double prixMax,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "") String search) {

        Sort sortObj = switch (sort != null ? sort : "") {
            case "prix_asc"  -> Sort.by("prix").ascending();
            case "prix_desc" -> Sort.by("prix").descending();
            default           -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(
            annonceRepository.search(
                type, ville, quartier, categorie, etat,
                urgent, negociable, avecPhoto,
                prixMin, prixMax, search,
                StatutAnnonce.ACTIVE, pageable
            )
        );
    }

    // ── Détail + Incrément vues ───────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        return annonceRepository.findById(id).map(a -> {
            a.setNombreVues((a.getNombreVues() == null ? 0 : a.getNombreVues()) + 1);
            annonceRepository.save(a);
            return ResponseEntity.ok(a);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Mes annonces ─────────────────────────────────────────
    @GetMapping("/mes-annonces")
    public ResponseEntity<?> mesAnnonces(@RequestHeader("Authorization") String auth) {
        User user = userFromToken(auth);
        if (user == null) return ResponseEntity.status(401).build();
        Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").descending());
        return ResponseEntity.ok(annonceRepository.findByProprietaireId(user.getId(), pageable));
    }

    // ── Créer ────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AnnonceRequest req,
                                    @RequestHeader("Authorization") String auth) {
        User user = userFromToken(auth);
        if (user == null) return ResponseEntity.status(401).build();

        Annonce a = Annonce.builder()
            .titre(req.getTitre())
            .description(req.getDescription())
            .type(req.getType())
            .prix(req.getPrix())
            .valeurEstimee(req.getValeurEstimee())
            .categorie(req.getCategorie())
            .etat(req.getEtat())
            .ville(req.getVille() != null ? req.getVille() : user.getVille())
            .quartier(req.getQuartier() != null ? req.getQuartier() : user.getQuartier())
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .urgent(req.getUrgent() != null && req.getUrgent())
            .negociable(req.getNegociable() != null && req.getNegociable())
            .rechercheContre(req.getRechercheContre() != null ? req.getRechercheContre() : new java.util.ArrayList<>())
            .photosUrls(req.getPhotosUrls() != null ? req.getPhotosUrls() : new java.util.ArrayList<>())
            .proprietaire(user)
            .build();

        // Géocode automatiquement si pas de coords fournies
        if (a.getLatitude() == null || a.getLongitude() == null) {
            double[] coords = geocode(a.getVille(), a.getQuartier());
            if (coords != null) {
                a.setLatitude(coords[0]);
                a.setLongitude(coords[1]);
            }
        }

        return ResponseEntity.ok(annonceRepository.save(a));
    }

    // ── Modifier ──────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody AnnonceRequest req,
                                    @RequestHeader("Authorization") String auth) {
        User user = userFromToken(auth);
        if (user == null) return ResponseEntity.status(401).build();

        return annonceRepository.findById(id).map(a -> {
            if (!a.getProprietaire().getId().equals(user.getId()))
                return ResponseEntity.status(403).build();

            if (req.getTitre()       != null) a.setTitre(req.getTitre());
            if (req.getDescription() != null) a.setDescription(req.getDescription());
            if (req.getPrix()        != null) a.setPrix(req.getPrix());
            if (req.getValeurEstimee() != null) a.setValeurEstimee(req.getValeurEstimee());
            if (req.getCategorie()   != null) a.setCategorie(req.getCategorie());
            if (req.getEtat()        != null) a.setEtat(req.getEtat());
            if (req.getVille()       != null) a.setVille(req.getVille());
            if (req.getQuartier()    != null) a.setQuartier(req.getQuartier());
            if (req.getLatitude()    != null) a.setLatitude(req.getLatitude());
            if (req.getLongitude()   != null) a.setLongitude(req.getLongitude());
            if (req.getPhotosUrls()  != null) a.setPhotosUrls(req.getPhotosUrls());
            if (req.getUrgent()      != null) a.setUrgent(req.getUrgent());
            if (req.getNegociable()  != null) a.setNegociable(req.getNegociable());
            
            a.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(annonceRepository.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Favori Toggle ─────────────────────────────────────────
    @PostMapping("/{id}/favori")
    public ResponseEntity<?> toggleFavori(@PathVariable Long id,
                                          @RequestHeader("Authorization") String auth) {
        User user = userFromToken(auth);
        if (user == null) return ResponseEntity.status(401).build();

        return annonceRepository.findById(id).map(a -> {
            boolean estFavori = a.getFavorisUsers().stream()
                .anyMatch(u -> u.getId().equals(user.getId()));

            if (estFavori) {
                a.getFavorisUsers().removeIf(u -> u.getId().equals(user.getId()));
                a.setNombreFavoris(Math.max(0, (a.getNombreFavoris() == null ? 0 : a.getNombreFavoris()) - 1));
            } else {
                a.getFavorisUsers().add(user);
                a.setNombreFavoris((a.getNombreFavoris() == null ? 0 : a.getNombreFavoris()) + 1);
            }
            annonceRepository.save(a);
            return ResponseEntity.ok(Map.of("favori", !estFavori, "nombreFavoris", a.getNombreFavoris()));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Annonces Similaires ───────────────────────────────────
    @GetMapping("/{id}/similaires")
    public ResponseEntity<?> similaires(@PathVariable Long id) {
        return annonceRepository.findById(id).map(a -> {
            Pageable p = PageRequest.of(0, 6, Sort.by("createdAt").descending());
            Page<Annonce> similar = annonceRepository.search(
                a.getType(), a.getVille(), null, a.getCategorie(),
                null, null, null, null, null, null, "",
                StatutAnnonce.ACTIVE, p
            );
            var list = similar.getContent().stream()
                .filter(s -> !s.getId().equals(id)).limit(5).toList();
            return ResponseEntity.ok(list);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Statut / Supprimer / Stats ─────────────────────────────
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> updateStatut(@PathVariable Long id,
                                          @RequestParam StatutAnnonce statut,
                                          @RequestHeader("Authorization") String auth) {
        User user = userFromToken(auth);
        if (user == null) return ResponseEntity.status(401).build();

        return annonceRepository.findById(id).map(a -> {
            if (!a.getProprietaire().getId().equals(user.getId())) return ResponseEntity.status(403).build();
            a.setStatut(statut);
            a.setUpdatedAt(LocalDateTime.now());
            return ResponseEntity.ok(annonceRepository.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestHeader("Authorization") String auth) {
        User user = userFromToken(auth);
        if (user == null) return ResponseEntity.status(401).build();

        return annonceRepository.findById(id).map(a -> {
            if (!a.getProprietaire().getId().equals(user.getId())) return ResponseEntity.status(403).build();
            annonceRepository.delete(a);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        long total = annonceRepository.countByStatut(StatutAnnonce.ACTIVE);
        return ResponseEntity.ok(Map.of("total", total));
    }

    // ── Helper ────────────────────────────────────────────────
    private User userFromToken(String auth) {
        try {
            if (auth == null || !auth.startsWith("Bearer ")) return null;
            String token = auth.substring(7);
            String subject = jwtUtil.extractEmail(token);
            return userRepository.findByEmail(subject)
                .or(() -> userRepository.findByTelephone(subject))
                .orElse(null);
        } catch (Exception e) { return null; }
    }
}