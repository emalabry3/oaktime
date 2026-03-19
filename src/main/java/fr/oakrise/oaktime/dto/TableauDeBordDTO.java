package fr.oakrise.oaktime.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO contenant toutes les données calculées pour le tableau de bord
 * d'une tâche (portant sur l'ensemble de ses descendants récursifs).
 */
@Getter
@Setter
@NoArgsConstructor
public class TableauDeBordDTO {

    // -------------------------------------------------------------------------
    // Bloc 1 — Vue d'ensemble
    // -------------------------------------------------------------------------

    /** Nombre total de descendants (toutes profondeurs). */
    private int totalDescendants;

    private int totalBacklog;
    private int totalEnCours;
    private int totalTermine;

    /** Pourcentage de tâches terminées (0-100). */
    private int pourcentageTermine;

    // -------------------------------------------------------------------------
    // Bloc 2 — Tâches en retard de démarrage
    // -------------------------------------------------------------------------

    /** Tâches dont dateDebut est dépassée et statut == BACKLOG. */
    private List<TacheDTO> tachesEnRetardDemarrage = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Bloc 3 — Échéances proches (≤ 7 jours)
    // -------------------------------------------------------------------------

    /** Tâches EN_COURS dont dateFinEffective est dans les 7 prochains jours (ou dépassée). */
    private List<TacheDTO> tachesEcheanceProche = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Bloc 4 — Tâches bloquées
    // -------------------------------------------------------------------------

    /** Tâches BACKLOG ou EN_COURS ayant au moins un prédécesseur non TERMINE. */
    private List<TacheDTO> tachesBloquees = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Bloc 5 — Activité récente (5 dernières créées)
    // -------------------------------------------------------------------------

    private List<TacheDTO> activiteRecente = new ArrayList<>();
}