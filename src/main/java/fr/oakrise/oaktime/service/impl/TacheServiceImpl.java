package fr.oakrise.oaktime.service.impl;

import fr.oakrise.oaktime.dto.TacheDTO;
import fr.oakrise.oaktime.entity.StatutKanban;
import fr.oakrise.oaktime.entity.Tache;
import fr.oakrise.oaktime.entity.TypeTrace;
import fr.oakrise.oaktime.exception.TacheNotFoundException;
import fr.oakrise.oaktime.mapper.TacheMapper;
import fr.oakrise.oaktime.repository.TacheRepository;
import fr.oakrise.oaktime.service.TacheService;
import fr.oakrise.oaktime.service.TraceService;
import fr.oakrise.oaktime.service.component.TacheValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TacheServiceImpl implements TacheService {

    private final TacheRepository tacheRepository;
    private final TacheMapper tacheMapper;
    private final TacheValidationService tacheValidationService;
    private final TraceService traceService;

    // =========================================================================
    // LECTURE
    // =========================================================================

    @Override @Transactional(readOnly = true)
    public List<TacheDTO> listerTachesRacines() {
        return tacheMapper.toDTOList(
            tacheRepository.findByParentIsNullOrderByDateCreationAsc());
    }

    @Override @Transactional(readOnly = true)
    public TacheDTO trouverParId(Long id) {
        return tacheMapper.toDTO(getTacheOuException(id));
    }

    @Override @Transactional(readOnly = true)
    public TacheDTO trouverParIdAvecSousTaches(Long id) {
        Tache tache = tacheRepository.findByIdWithSousTaches(id)
                .orElseThrow(() -> new TacheNotFoundException(id));
        return tacheMapper.toDTOAvecSousTaches(tache);
    }

    @Override @Transactional(readOnly = true)
    public List<TacheDTO> rechercherParNom(String nom) {
        return tacheMapper.toDTOList(
            tacheRepository.findByParentIsNullAndNomContainingIgnoreCase(nom));
    }

    // =========================================================================
    // CRÉATION
    // =========================================================================

    @Override @Transactional
    public TacheDTO creer(TacheDTO dto) {
        tacheValidationService.valider(dto, null);
        Tache tache = tacheMapper.toEntity(dto);
        Tache saved = tacheRepository.save(tache);
        traceService.tracer(saved.getId(), saved.getNom(),
            TypeTrace.CREATION, "Tâche créée : \"" + saved.getNom() + "\"");
        return tacheMapper.toDTO(saved);
    }

    @Override @Transactional
    public TacheDTO creerSousTache(TacheDTO dto, Long parentId) {
        tacheValidationService.valider(dto, parentId);
        Tache parent = getTacheOuException(parentId);
        Tache sousTache = tacheMapper.toEntity(dto);
        parent.ajouterSousTache(sousTache);
        tacheRepository.save(parent);
        traceService.tracer(sousTache.getId(), sousTache.getNom(),
            TypeTrace.CREATION, "Tâche créée : \"" + sousTache.getNom() + "\"");
        traceService.tracer(parentId, parent.getNom(),
            TypeTrace.SOUS_TACHE, "Nouvelle sous-tâche ajoutée : \"" + sousTache.getNom() + "\"");
        return tacheMapper.toDTO(sousTache);
    }

    // =========================================================================
    // MODIFICATION — avec R7, R8
    // =========================================================================

    @Override @Transactional
    public TacheDTO modifier(Long id, TacheDTO dto) {
        Tache tache = getTacheOuException(id);
        Long parentId = tache.getParent() != null ? tache.getParent().getId() : null;

        // R7 / R8 : vérifier que les dates immutables ne changent pas
        TacheDTO actuelle = tacheMapper.toDTO(tache);
        tacheValidationService.validerModificationDates(tache.getStatutKanban(), dto, actuelle);

        // R1 / R2
        tacheValidationService.valider(dto, parentId);

        tacheMapper.mergeToEntity(dto, tache);
        Tache saved = tacheRepository.save(tache);
        traceService.tracer(id, saved.getNom(),
            TypeTrace.MODIFICATION, "Tâche modifiée : \"" + saved.getNom() + "\"");
        return tacheMapper.toDTO(saved);
    }

    // =========================================================================
    // SUPPRESSION
    // =========================================================================

    @Override @Transactional
    public void supprimer(Long id) {
        tacheRepository.delete(getTacheOuException(id));
    }

    // =========================================================================
    // CHANGEMENT DE STATUT — R3, R4, R5, propagation ascendante
    // =========================================================================

    @Override @Transactional
    public List<String> changerStatut(Long id, StatutKanban statut) {
        log.info("Changement statut tâche id={} → {}", id, statut);
        Tache tache = getTacheOuException(id);
        List<String> autoTermines = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // R5 : vérifier que tous les prédécesseurs sont TERMINE avant EN_COURS / TERMINE
        if (statut == StatutKanban.EN_COURS || statut == StatutKanban.TERMINE) {
            boolean predecesseurNonTermine = tache.getPredecesseurs().stream()
                    .anyMatch(p -> p.getStatutKanban() != StatutKanban.TERMINE);
            if (predecesseurNonTermine) {
                throw new fr.oakrise.oaktime.exception.TacheValidationException(
                    "R5 — Impossible de " + (statut == StatutKanban.EN_COURS ? "démarrer" : "terminer") +
                    " cette tâche : un ou plusieurs prédécesseurs ne sont pas encore terminés.");
            }
        }

        // R3 : passage EN_COURS → dateDebut = aujourd'hui (si pas déjà renseignée)
        if (statut == StatutKanban.EN_COURS && tache.getDateDebut() == null) {
            tache.setDateDebut(today);
            log.info("R3 — dateDebut fixée à {} pour tâche id={}", today, id);
            traceService.tracer(id, tache.getNom(), TypeTrace.MODIFICATION,
                "R3 — Date de début fixée automatiquement : " + today);
        }

        // R4 : passage TERMINE → dateFin = aujourd'hui
        if (statut == StatutKanban.TERMINE) {
            tache.setDateFin(today);
            log.info("R4 — dateFin fixée à {} pour tâche id={}", today, id);
            traceService.tracer(id, tache.getNom(), TypeTrace.MODIFICATION,
                "R4 — Date de fin fixée automatiquement : " + today);
        }

        tache.setStatutKanban(statut);
        tacheRepository.save(tache);

        String libelle = switch (statut) {
            case BACKLOG   -> "Backlog";
            case EN_COURS  -> "En cours";
            case TERMINE   -> "Terminé";
        };
        traceService.tracer(id, tache.getNom(), TypeTrace.STATUT, "Statut changé → " + libelle);

        // Propagation EN_COURS vers ascendants
        if (statut == StatutKanban.EN_COURS) {
            Tache parent = tache.getParent();
            while (parent != null) {
                if (parent.getStatutKanban() != StatutKanban.EN_COURS) {
                    // R3 ascendant aussi
                    if (parent.getDateDebut() == null) parent.setDateDebut(today);
                    parent.setStatutKanban(StatutKanban.EN_COURS);
                    tacheRepository.save(parent);
                }
                parent = parent.getParent();
            }
        }

        // Propagation TERMINE vers ascendants si tous descendants terminés
        if (statut == StatutKanban.TERMINE) {
            Tache parent = tache.getParent();
            while (parent != null) {
                if (tousDescendantsTermines(parent)) {
                    parent.setStatutKanban(StatutKanban.TERMINE);
                    parent.setDateFin(today); // R4 ascendant
                    tacheRepository.save(parent);
                    autoTermines.add(parent.getNom());
                    traceService.tracer(parent.getId(), parent.getNom(), TypeTrace.STATUT,
                        "Statut changé automatiquement → Terminé (toutes les sous-tâches sont terminées)");
                    traceService.tracer(parent.getId(), parent.getNom(), TypeTrace.MODIFICATION,
                        "R4 — Date de fin fixée automatiquement : " + today);
                } else break;
                parent = parent.getParent();
            }
        }

        return autoTermines;
    }

    // =========================================================================
    // R5 / R6 — Impact sur les successeurs
    // =========================================================================

    /**
     * R6 — Vérifie si la nouvelle dateFin de tacheId violerait R5 pour ses successeurs.
     * Retourne les noms des successeurs impactés (dateDebut < nouvelle dateFin).
     */
    @Override @Transactional(readOnly = true)
    public List<String> verifierImpactSuccesseurs(Long tacheId, LocalDate nouvelleDataFin) {
        if (nouvelleDataFin == null) return Collections.emptyList();
        Tache tache = getTacheOuException(tacheId);
        return tache.getSuccesseurs().stream()
                .filter(s -> s.getDateDebut() != null && s.getDateDebut().isBefore(nouvelleDataFin))
                .map(Tache::getNom)
                .collect(Collectors.toList());
    }

    /**
     * R6 — Décale les dateDebut des successeurs (récursivement) pour respecter R5.
     */
    @Override @Transactional
    public void decalerSuccesseurs(Long tacheId) {
        Tache tache = getTacheOuException(tacheId);
        if (tache.getDateFin() == null) return;
        decalerRecursivement(tache, tache.getDateFin());
    }

    private void decalerRecursivement(Tache predecesseur, LocalDate dateFinPred) {
        for (Tache successeur : predecesseur.getSuccesseurs()) {
            if (successeur.getDateDebut() != null
                    && successeur.getDateDebut().isBefore(dateFinPred)) {
                LocalDate ancienneDebut = successeur.getDateDebut();
                long decalage = dateFinPred.toEpochDay() - ancienneDebut.toEpochDay();
                LocalDate nouvelleDebut = dateFinPred;
                successeur.setDateDebut(nouvelleDebut);
                // Décaler dateFin proportionnellement si elle existe
                if (successeur.getDateFin() != null) {
                    successeur.setDateFin(successeur.getDateFin().plusDays(decalage));
                }
                tacheRepository.save(successeur);
                traceService.tracer(successeur.getId(), successeur.getNom(),
                    TypeTrace.MODIFICATION,
                    "R6 — Dates décalées automatiquement : début " + ancienneDebut +
                    " → " + nouvelleDebut);
                // Récursion sur les successeurs du successeur
                decalerRecursivement(successeur, successeur.getDateFin() != null
                    ? successeur.getDateFin() : successeur.getDateDebut());
            }
        }
    }

    // =========================================================================
    // PRÉDÉCESSEURS
    // =========================================================================

    @Override @Transactional(readOnly = true)
    public List<TacheDTO> listerSoeursEligibles(Long tacheId) {
        Tache tache = tacheRepository.findByIdWithPredecesseurs(tacheId)
                .orElseThrow(() -> new TacheNotFoundException(tacheId));

        List<Tache> soeurs = tache.getParent() != null
                ? tacheRepository.findByParentIdOrderByDateCreationAsc(tache.getParent().getId())
                : tacheRepository.findByParentIsNullOrderByDateCreationAsc();

        Set<Long> dejaPred = tache.getPredecesseurs().stream()
                .map(Tache::getId).collect(Collectors.toSet());

        return soeurs.stream()
                .filter(s -> !s.getId().equals(tacheId))
                .filter(s -> !dejaPred.contains(s.getId()))
                .filter(s -> !creeraitUnCycle(tache, s))
                .map(tacheMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override @Transactional
    public void ajouterPredecesseur(Long tacheId, Long predecesseurId) {
        Tache tache = tacheRepository.findByIdWithPredecesseurs(tacheId)
                .orElseThrow(() -> new TacheNotFoundException(tacheId));
        Tache predecesseur = tacheRepository.findByIdWithPredecesseurs(predecesseurId)
                .orElseThrow(() -> new TacheNotFoundException(predecesseurId));

        // Même parent
        boolean memeParent = Objects.equals(
                tache.getParent() != null ? tache.getParent().getId() : null,
                predecesseur.getParent() != null ? predecesseur.getParent().getId() : null);
        if (!memeParent)
            throw new fr.oakrise.oaktime.exception.TacheValidationException(
                "Un prédécesseur doit être une tâche sœur (même parent).");
        if (tacheId.equals(predecesseurId))
            throw new fr.oakrise.oaktime.exception.TacheValidationException(
                "Une tâche ne peut pas être son propre prédécesseur.");
        if (creeraitUnCycle(tache, predecesseur))
            throw new fr.oakrise.oaktime.exception.TacheValidationException(
                "Ajout impossible : créerait une dépendance circulaire.");

        tache.ajouterPredecesseur(predecesseur);

        // R5 : ajuster dateDebut de la tâche si nécessaire
        LocalDate dateMiniDebut = tache.getPredecesseurs().stream()
                .map(Tache::getDateFin)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (dateMiniDebut != null
                && (tache.getDateDebut() == null || tache.getDateDebut().isBefore(dateMiniDebut))) {
            log.info("R5 — dateDebut tâche id={} ajustée → {}", tacheId, dateMiniDebut);
            tache.setDateDebut(dateMiniDebut);
            traceService.tracer(tacheId, tache.getNom(), TypeTrace.MODIFICATION,
                "R5 — Date de début ajustée → " + dateMiniDebut +
                " (contrainte prédécesseur \"" + predecesseur.getNom() + "\")");
        }

        tacheRepository.save(tache);
        traceService.tracer(tacheId, tache.getNom(), TypeTrace.PREDECESSEUR,
            "Prédécesseur ajouté : \"" + predecesseur.getNom() + "\"");
        traceService.tracer(predecesseurId, predecesseur.getNom(), TypeTrace.PREDECESSEUR,
            "Ajouté comme prédécesseur de : \"" + tache.getNom() + "\"");
    }

    @Override @Transactional
    public void supprimerPredecesseur(Long tacheId, Long predecesseurId) {
        Tache tache = tacheRepository.findByIdWithPredecesseurs(tacheId)
                .orElseThrow(() -> new TacheNotFoundException(tacheId));
        Tache predecesseur = getTacheOuException(predecesseurId);
        tache.retirerPredecesseur(predecesseur);
        tacheRepository.save(tache);
        traceService.tracer(tacheId, tache.getNom(), TypeTrace.PREDECESSEUR,
            "Prédécesseur retiré : \"" + predecesseur.getNom() + "\"");
    }

    // =========================================================================
    // TABLEAU DE BORD
    // =========================================================================

    @Override @Transactional(readOnly = true)
    public fr.oakrise.oaktime.dto.TableauDeBordDTO calculerTableauDeBord(Long tacheId) {
        Tache racine = getTacheOuException(tacheId);
        LocalDate today = LocalDate.now();
        LocalDate dans7Jours = today.plusDays(7);

        List<Tache> tous = new ArrayList<>();
        collecterDescendants(racine, tous);

        fr.oakrise.oaktime.dto.TableauDeBordDTO tdb = new fr.oakrise.oaktime.dto.TableauDeBordDTO();
        tdb.setTotalDescendants(tous.size());

        int backlog = 0, enCours = 0, termine = 0;
        List<Tache> retardDemarrage = new ArrayList<>();
        List<Tache> echeanceProche  = new ArrayList<>();
        List<Tache> bloquees        = new ArrayList<>();

        for (Tache t : tous) {
            StatutKanban sk = t.getStatutKanban();
            if (sk == StatutKanban.BACKLOG)   backlog++;
            else if (sk == StatutKanban.EN_COURS) enCours++;
            else if (sk == StatutKanban.TERMINE)  termine++;

            // Retard démarrage : BACKLOG avec dateDebut dépassée
            if (sk == StatutKanban.BACKLOG && t.getDateDebut() != null
                    && t.getDateDebut().isBefore(today)) {
                retardDemarrage.add(t);
            }

            // Échéance proche : EN_COURS avec dateFin dans les 7 jours ou dépassée
            if (sk == StatutKanban.EN_COURS && t.getDateFin() != null
                    && !t.getDateFin().isAfter(dans7Jours)) {
                echeanceProche.add(t);
            }

            // Bloquée : prédécesseur non TERMINE
            if (sk != StatutKanban.TERMINE) {
                boolean bloque = t.getPredecesseurs().stream()
                        .anyMatch(p -> p.getStatutKanban() != StatutKanban.TERMINE);
                if (bloque) bloquees.add(t);
            }
        }

        tdb.setTotalBacklog(backlog);
        tdb.setTotalEnCours(enCours);
        tdb.setTotalTermine(termine);
        tdb.setPourcentageTermine(tous.isEmpty() ? 0 : (int) Math.round(100.0 * termine / tous.size()));

        retardDemarrage.sort(Comparator.comparing(Tache::getDateDebut));
        tdb.setTachesEnRetardDemarrage(tacheMapper.toDTOList(retardDemarrage));

        echeanceProche.sort(Comparator.comparing(Tache::getDateFin));
        tdb.setTachesEcheanceProche(tacheMapper.toDTOList(echeanceProche));
        tdb.setTachesBloquees(tacheMapper.toDTOList(bloquees));

        tous.sort(Comparator.comparing(Tache::getDateCreation).reversed());
        tdb.setActiviteRecente(tacheMapper.toDTOList(tous.subList(0, Math.min(5, tous.size()))));

        return tdb;
    }

    // =========================================================================
    // UTILITAIRES PRIVÉES
    // =========================================================================

    private boolean tousDescendantsTermines(Tache tache) {
        for (Tache enfant : tache.getSousTaches()) {
            if (enfant.getStatutKanban() != StatutKanban.TERMINE) return false;
            if (!tousDescendantsTermines(enfant)) return false;
        }
        return true;
    }

    private void collecterDescendants(Tache tache, List<Tache> resultat) {
        for (Tache enfant : tache.getSousTaches()) {
            resultat.add(enfant);
            collecterDescendants(enfant, resultat);
        }
    }

    private boolean creeraitUnCycle(Tache tache, Tache candidat) {
        Set<Long> visites = new HashSet<>();
        Deque<Tache> pile = new ArrayDeque<>();
        pile.push(candidat);
        while (!pile.isEmpty()) {
            Tache courant = pile.pop();
            if (courant.getId().equals(tache.getId())) return true;
            if (visites.add(courant.getId())) {
                tacheRepository.findByIdWithPredecesseurs(courant.getId())
                        .ifPresent(t -> t.getPredecesseurs().forEach(pile::push));
            }
        }
        return false;
    }

    private Tache getTacheOuException(Long id) {
        return tacheRepository.findById(id)
                .orElseThrow(() -> new TacheNotFoundException(id));
    }
}