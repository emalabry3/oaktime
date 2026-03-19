package fr.oakrise.oaktime.controller;

import fr.oakrise.oaktime.dto.DateUpdateResponseDTO;
import fr.oakrise.oaktime.dto.StatutChangeResponseDTO;
import fr.oakrise.oaktime.dto.TacheDTO;
import fr.oakrise.oaktime.service.TacheService;
import fr.oakrise.oaktime.service.TraceService;
import fr.oakrise.oaktime.entity.TypeTrace;
import fr.oakrise.oaktime.dto.TraceTacheDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller Spring MVC gérant les vues liées aux tâches.
 *
 * Routes :
 *  GET  /taches              → liste des tâches racines
 *  GET  /taches/nouveau      → formulaire création
 *  POST /taches/nouveau      → créer
 *  GET  /taches/{id}         → détail/édition + sous-tâches en panels
 *  POST /taches/{id}         → enregistrer modifications
 *  POST /taches/{id}/supprimer → supprimer
 */
@Slf4j
@Controller
@RequestMapping("/taches")
@RequiredArgsConstructor
public class TacheController {

    private final TacheService tacheService;
    private final TraceService traceService;

    // =========================================================================
    // LISTE
    // =========================================================================

    @GetMapping
    public String liste(
            @RequestParam(name = "recherche", required = false) String recherche,
            Model model) {

        if (recherche != null && !recherche.isBlank()) {
            List<TacheDTO> taches = tacheService.rechercherParNom(recherche);
            enrichirPourAffichage(taches);
            taches = trierTopologique(taches);
            model.addAttribute("taches", taches);
            model.addAttribute("recherche", recherche);
        } else {
            List<TacheDTO> taches = tacheService.listerTachesRacines();
            enrichirPourAffichage(taches);
            taches = trierTopologique(taches);
            model.addAttribute("taches", taches);
        }
        return "taches/liste";
    }

    // =========================================================================
    // CRÉATION
    // =========================================================================

    @GetMapping("/nouveau")
    public String nouveauFormulaire(
            @RequestParam(name = "parentId", required = false) Long parentId,
            Model model) {

        TacheDTO dto = new TacheDTO();

        if (parentId != null) {
            TacheDTO parent = tacheService.trouverParId(parentId);
            dto.setParentId(parentId);
            dto.setParentNom(parent.getNom());
            model.addAttribute("parent", parent);
            model.addAttribute("ancetres", construireAncetres(parentId));
            model.addAttribute("dateMin", parent.getDateDebut() != null
                    ? parent.getDateDebut().toString() : null);
            model.addAttribute("dateMax", parent.getDateFin() != null ? parent.getDateFin().toString() : null);
        } else {
            model.addAttribute("parent", null);
            model.addAttribute("ancetres", List.of());
            model.addAttribute("dateMin", null);
            model.addAttribute("dateMax", null);
        }

        model.addAttribute("tache", dto);
        model.addAttribute("modeEdition", false);
        model.addAttribute("urlRetour", parentId != null ? "/taches/" + parentId : "/taches");
        return "taches/formulaire";
    }

    @PostMapping("/nouveau")
    public String creer(
            @Valid @ModelAttribute("tache") TacheDTO dto,
            BindingResult bindingResult,
            @RequestParam(name = "parentId", required = false) Long parentId,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (parentId != null) dto.setParentId(parentId);

        if (bindingResult.hasErrors()) {
            model.addAttribute("modeEdition", false);
            model.addAttribute("urlRetour", parentId != null ? "/taches/" + parentId : "/taches");
            if (parentId != null) {
                TacheDTO parent = tacheService.trouverParId(parentId);
                model.addAttribute("parent", parent);
                model.addAttribute("ancetres", construireAncetres(parentId));
                model.addAttribute("dateMin", parent.getDateDebut() != null
                        ? parent.getDateDebut().toString() : null);
                model.addAttribute("dateMax", parent.getDateFin() != null
                        ? parent.getDateFin().toString() : null);
            } else {
                model.addAttribute("parent", null);
                model.addAttribute("ancetres", List.of());
                model.addAttribute("dateMin", null);
                model.addAttribute("dateMax", null);
            }
            return "taches/formulaire";
        }

        if (parentId != null) {
            TacheDTO creee = tacheService.creerSousTache(dto, parentId);
            redirectAttributes.addFlashAttribute("messageSucces",
                    "Sous-tâche \"" + creee.getNom() + "\" créée avec succès.");
            return "redirect:/taches/" + parentId;
        } else {
            TacheDTO creee = tacheService.creer(dto);
            redirectAttributes.addFlashAttribute("messageSucces",
                    "Tâche \"" + creee.getNom() + "\" créée avec succès.");
            return "redirect:/taches";
        }
    }

    // =========================================================================
    // DÉTAIL / ÉDITION
    // =========================================================================

    @GetMapping("/{id}")
    public String detail(
            @PathVariable(name = "id") Long id,
            Model model) {

        TacheDTO tache = tacheService.trouverParId(id);
        List<TacheDTO> sousTaches = tacheService.trouverParIdAvecSousTaches(id).getSousTaches();
        enrichirPourAffichage(sousTaches);
        sousTaches = trierTopologique(sousTaches);

        model.addAttribute("tache", tache);
        model.addAttribute("sousTaches", sousTaches);
        model.addAttribute("ancetres", construireAncetres(id));
        enrichirPourAffichage(tache.getPredecesseurs());
        enrichirPourAffichage(tache.getSuccesseurs());
        ajouterDatesFormatees(tache, model);

        if (tache.getParentId() != null) {
            TacheDTO parent = tacheService.trouverParId(tache.getParentId());
            model.addAttribute("dateMin", parent.getDateDebut() != null
                    ? parent.getDateDebut().toString() : null);
            model.addAttribute("dateMax", parent.getDateFin() != null ? parent.getDateFin().toString() : null);
        } else {
            model.addAttribute("dateMin", null);
            model.addAttribute("dateMax", null);
        }

        model.addAttribute("urlRetour", tache.getParentId() != null
                ? "/taches/" + tache.getParentId() : "/taches");
        List<TacheDTO> soeursEligibles = tacheService.listerSoeursEligibles(id);
        enrichirPourAffichage(soeursEligibles);
        model.addAttribute("soeursEligibles", soeursEligibles);

        fr.oakrise.oaktime.dto.TableauDeBordDTO tdb = tacheService.calculerTableauDeBord(id);
        enrichirPourAffichage(tdb.getTachesEnRetardDemarrage());
        enrichirPourAffichage(tdb.getTachesEcheanceProche());
        enrichirPourAffichage(tdb.getTachesBloquees());
        enrichirPourAffichage(tdb.getActiviteRecente());
        model.addAttribute("tdb", tdb);

        List<TraceTacheDTO> traces;
        try {
            traces = traceService.listerParTache(id);
        } catch (Exception e) {
            log.warn("Impossible de charger les traces pour tâche id={} : {}", id, e.getMessage());
            traces = java.util.Collections.emptyList();
        }
        if (traces == null) traces = java.util.Collections.emptyList();
        model.addAttribute("traces", traces);

        return "taches/detail";
    }

    @PostMapping("/{id}")
    public String modifier(
            @PathVariable(name = "id") Long id,
            @Valid @ModelAttribute("tache") TacheDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            TacheDTO actuelle = tacheService.trouverParId(id);
            List<TacheDTO> sousTachesErreur = tacheService.trouverParIdAvecSousTaches(id).getSousTaches();
            enrichirPourAffichage(sousTachesErreur);
            model.addAttribute("sousTaches", sousTachesErreur);
            model.addAttribute("ancetres", construireAncetres(id));
            ajouterDatesFormatees(tacheService.trouverParId(id), model);
            model.addAttribute("urlRetour", actuelle.getParentId() != null
                    ? "/taches/" + actuelle.getParentId() : "/taches");
            if (actuelle.getParentId() != null) {
                TacheDTO parent = tacheService.trouverParId(actuelle.getParentId());
                model.addAttribute("dateMin", parent.getDateDebut() != null
                        ? parent.getDateDebut().toString() : null);
                model.addAttribute("dateMax", parent.getDateFin() != null
                        ? parent.getDateFin().toString() : null);
            } else {
                model.addAttribute("dateMin", null);
                model.addAttribute("dateMax", null);
            }
            return "taches/detail";
        }

        TacheDTO modifiee = tacheService.modifier(id, dto);
        redirectAttributes.addFlashAttribute("messageSucces",
                "Tâche \"" + modifiee.getNom() + "\" modifiée avec succès.");
        return "redirect:/taches/" + id;
    }

    // =========================================================================
    // PRÉDÉCESSEURS
    // =========================================================================

    @PostMapping("/{id}/predecesseurs/ajouter")
    public String ajouterPredecesseur(
            @PathVariable("id") Long id,
            @RequestParam("predecesseurId") Long predecesseurId,
            RedirectAttributes redirectAttributes) {

        try {
            tacheService.ajouterPredecesseur(id, predecesseurId);
            redirectAttributes.addFlashAttribute("messageSucces", "Prédécesseur ajouté.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("messageErreur", e.getMessage());
        }
        return "redirect:/taches/" + id;
    }

    @PostMapping("/{id}/predecesseurs/supprimer")
    public String supprimerPredecesseur(
            @PathVariable("id") Long id,
            @RequestParam("predecesseurId") Long predecesseurId,
            RedirectAttributes redirectAttributes) {

        try {
            tacheService.supprimerPredecesseur(id, predecesseurId);
            redirectAttributes.addFlashAttribute("messageSucces", "Prédécesseur retiré.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("messageErreur", e.getMessage());
        }
        return "redirect:/taches/" + id;
    }

    // =========================================================================
    // SUPPRESSION
    // =========================================================================

    @PostMapping("/{id}/supprimer")
    public String supprimer(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "parentId", required = false) Long parentId,
            RedirectAttributes redirectAttributes) {

        TacheDTO tache = tacheService.trouverParId(id);
        tacheService.supprimer(id);

        redirectAttributes.addFlashAttribute("messageSucces",
                "Tâche \"" + tache.getNom() + "\" supprimée avec succès.");

        return parentId != null
                ? "redirect:/taches/" + parentId
                : "redirect:/taches";
    }

    // =========================================================================
    // JOURNAL DE TRAÇAGE
    // =========================================================================

    @PostMapping("/{id}/traces")
    public String ajouterTrace(
            @PathVariable("id") Long id,
            @RequestParam("message") String message,
            RedirectAttributes redirectAttributes) {

        if (message == null || message.isBlank()) {
            redirectAttributes.addFlashAttribute("messageErreur", "Le message ne peut pas être vide.");
            return "redirect:/taches/" + id + "#tab-journal";
        }
        TacheDTO tache = tacheService.trouverParId(id);
        traceService.tracer(id, tache.getNom(), TypeTrace.MANUEL, message.trim());
        redirectAttributes.addFlashAttribute("messageSucces", "Note ajoutée.");
        return "redirect:/taches/" + id + "#tab-journal";
    }

    @PostMapping("/traces/{traceId}/supprimer")
    public String supprimerTrace(
            @PathVariable("traceId") Long traceId,
            @RequestParam("tacheId") Long tacheId,
            RedirectAttributes redirectAttributes) {

        try {
            traceService.supprimerTrace(traceId);
            redirectAttributes.addFlashAttribute("messageSucces", "Note supprimée.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("messageErreur", e.getMessage());
        }
        return "redirect:/taches/" + tacheId + "#tab-journal";
    }

    // =========================================================================
    // DATES — mise à jour inline (onglet Dates) — R1, R6, R7, R8
    // =========================================================================

    /**
     * Met à jour dateDebut ou dateFin d'une tâche via AJAX.
     * R7 : EN_COURS → dateDebut non modifiable.
     * R8 : TERMINE  → dateDebut et dateFin non modifiables.
     * R6 : si la nouvelle dateFin viole R5 chez des successeurs, renvoie 409
     *      avec un {@link DateUpdateResponseDTO} listant les tâches impactées ;
     *      le client confirme puis appelle /dates/decaler si accord.
     */
    @PostMapping("/{id}/dates")
    @ResponseBody
    public ResponseEntity<DateUpdateResponseDTO> mettreAJourDate(
            @PathVariable("id") Long id,
            @RequestParam("champ") String champ,
            @RequestParam("valeur") String valeur) {
        try {
            TacheDTO tache = tacheService.trouverParId(id);
            java.time.LocalDate date = (valeur == null || valeur.isBlank())
                ? null : java.time.LocalDate.parse(valeur);

            if ("dateDebut".equals(champ)) {
                tache.setDateDebut(date);
            } else if ("dateFin".equals(champ)) {
                tache.setDateFin(date);
            } else {
                return ResponseEntity.badRequest()
                    .body(DateUpdateResponseDTO.erreur("Champ inconnu : " + champ));
            }

            // R1 vérification préalable
            if ("dateFin".equals(champ) && tache.getDateDebut() != null && date != null
                    && date.isBefore(tache.getDateDebut())) {
                return ResponseEntity.badRequest()
                    .body(DateUpdateResponseDTO.erreur(
                        "R1 — La date de fin ne peut pas être antérieure à la date de début."));
            }

            // R6 : détecter les successeurs impactés avant de sauvegarder
            if ("dateFin".equals(champ) && date != null) {
                java.util.List<String> impactes = tacheService.verifierImpactSuccesseurs(id, date);
                if (!impactes.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(DateUpdateResponseDTO.conflitR6(impactes));
                }
            }

            tacheService.modifier(id, tache);
            traceService.tracer(id, tache.getNom(), TypeTrace.MODIFICATION,
                "Date \"" + champ + "\" mise à jour → " + (date != null ? date : "—"));
            return ResponseEntity.ok(DateUpdateResponseDTO.ok());

        } catch (fr.oakrise.oaktime.exception.TacheValidationException e) {
            return ResponseEntity.badRequest()
                .body(DateUpdateResponseDTO.erreur(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(DateUpdateResponseDTO.erreur(e.getMessage()));
        }
    }

    /**
     * R6 — Décale les successeurs impactés après confirmation de l'utilisateur.
     */
    @PostMapping("/{id}/dates/decaler")
    @ResponseBody
    public ResponseEntity<DateUpdateResponseDTO> decalerSuccesseurs(
            @PathVariable("id") Long id,
            @RequestParam("valeur") String valeur) {
        try {
            TacheDTO tache = tacheService.trouverParId(id);
            java.time.LocalDate date = java.time.LocalDate.parse(valeur);
            tache.setDateFin(date);
            tacheService.modifier(id, tache);
            tacheService.decalerSuccesseurs(id);
            return ResponseEntity.ok(DateUpdateResponseDTO.ok());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(DateUpdateResponseDTO.erreur(e.getMessage()));
        }
    }

    // =========================================================================
    // KANBAN — changement de statut
    // =========================================================================

    /**
     * Met à jour le statut Kanban d'une tâche via un appel AJAX (drag & drop).
     * Retourne 200 OK avec {@link StatutChangeResponseDTO} si succès,
     * 409 si règle métier violée (R5), 400 si statut invalide.
     */
    @PostMapping("/{id}/statut")
    @ResponseBody
    public ResponseEntity<StatutChangeResponseDTO> changerStatut(
            @PathVariable("id") Long id,
            @RequestParam("statut") String statut) {

        try {
            fr.oakrise.oaktime.entity.StatutKanban sk =
                fr.oakrise.oaktime.entity.StatutKanban.valueOf(statut.toUpperCase());
            java.util.List<String> autoTermines = tacheService.changerStatut(id, sk);
            return ResponseEntity.ok(StatutChangeResponseDTO.succes(autoTermines));

        } catch (fr.oakrise.oaktime.exception.TacheValidationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(StatutChangeResponseDTO.erreur(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(StatutChangeResponseDTO.erreur("Statut invalide : " + statut));
        }
    }

    // =========================================================================
    // UTILITAIRES
    // =========================================================================

    private void enrichirPourAffichage(List<TacheDTO> taches) {
        java.time.format.DateTimeFormatter iso       = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.format.DateTimeFormatter affichage = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yy");

        for (TacheDTO t : taches) {
            java.time.LocalDate fin   = t.getDateFin();
            java.time.LocalDate debut = t.getDateDebut();

            t.setDateDebutStr(debut != null ? debut.format(iso) : "");
            t.setDateFinStr(fin != null ? fin.format(iso) : "");
            t.setDateDebutAffichage(debut != null ? debut.format(affichage) : "");
            t.setDateFinAffichage(fin != null ? fin.format(affichage) : "");
        }
    }

    private void ajouterDatesFormatees(TacheDTO tache, Model model) {
        java.time.format.DateTimeFormatter affichage = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        java.time.format.DateTimeFormatter iso       = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        model.addAttribute("dateDebutStr",
                tache.getDateDebut() != null ? tache.getDateDebut().format(iso) : "");
        model.addAttribute("dateFinStr",
                tache.getDateFin() != null ? tache.getDateFin().format(iso) : "");
        model.addAttribute("dateFinAffichage",
                tache.getDateFin() != null ? tache.getDateFin().format(affichage) : "—");
        model.addAttribute("dateCreationStr",
                tache.getDateCreation() != null
                        ? tache.getDateCreation().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "—");
    }

    private List<TacheDTO> trierTopologique(List<TacheDTO> taches) {
        java.util.Map<Long, TacheDTO> parId = new java.util.LinkedHashMap<>();
        for (TacheDTO t : taches) parId.put(t.getId(), t);

        java.util.Map<Long, Integer> degre = new java.util.LinkedHashMap<>();
        java.util.Map<Long, java.util.List<Long>> successeursMap = new java.util.LinkedHashMap<>();
        for (TacheDTO t : taches) {
            degre.putIfAbsent(t.getId(), 0);
            successeursMap.putIfAbsent(t.getId(), new java.util.ArrayList<>());
        }
        for (TacheDTO t : taches) {
            for (TacheDTO pred : t.getPredecesseurs()) {
                if (parId.containsKey(pred.getId())) {
                    degre.merge(t.getId(), 1, Integer::sum);
                    successeursMap.get(pred.getId()).add(t.getId());
                }
            }
        }

        java.util.Queue<Long> file = new java.util.ArrayDeque<>();
        for (java.util.Map.Entry<Long, Integer> e : degre.entrySet()) {
            if (e.getValue() == 0) file.add(e.getKey());
        }

        List<TacheDTO> tries = new java.util.ArrayList<>();
        while (!file.isEmpty()) {
            Long courant = file.poll();
            TacheDTO dto = parId.get(courant);
            if (dto != null) tries.add(dto);
            for (Long succId : successeursMap.getOrDefault(courant, java.util.Collections.emptyList())) {
                int nvDegre = degre.merge(succId, -1, Integer::sum);
                if (nvDegre == 0) file.add(succId);
            }
        }
        if (tries.size() < taches.size()) {
            for (TacheDTO t : taches) {
                if (!tries.contains(t)) tries.add(t);
            }
        }
        return tries;
    }

    private List<TacheDTO> construireAncetres(Long id) {
        List<TacheDTO> ancetres = new ArrayList<>();
        Long courantId = id;
        int securite = 0;
        while (courantId != null && securite++ < 20) {
            TacheDTO t = tacheService.trouverParId(courantId);
            if (t.getParentId() == null) break;
            ancetres.add(0, tacheService.trouverParId(t.getParentId()));
            courantId = t.getParentId();
        }
        return ancetres;
    }
}
