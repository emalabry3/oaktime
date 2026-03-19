package fr.oakrise.oaktime.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour le tableau de bord général — vue synthétique sur l'ensemble
 * des tâches (racines + toute leur descendance).
 */
@Getter
@Setter
@NoArgsConstructor
public class TableauDeBordGeneralDTO {

    // -------------------------------------------------------------------------
    // Bloc 1 — Compteurs globaux
    // -------------------------------------------------------------------------

    private int totalTaches;
    private int totalBacklog;
    private int totalEnCours;
    private int totalTermine;
    private int pourcentageTermine;

    // -------------------------------------------------------------------------
    // Bloc 2 — Tâches urgentes (retard + échéance proche + bloquées)
    // -------------------------------------------------------------------------

    /** Tâches BACKLOG dont dateDebut est dépassée. */
    private List<TacheUrgenceDTO> tachesEnRetardDemarrage = new ArrayList<>();

    /** Tâches EN_COURS dont dateFin est dans les 7 jours ou dépassée. */
    private List<TacheUrgenceDTO> tachesEcheanceProche = new ArrayList<>();

    /** Tâches bloquées par au moins un prédécesseur non TERMINE. */
    private List<TacheUrgenceDTO> tachesBloquees = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Bloc 3 — Répartition par tâche racine
    // -------------------------------------------------------------------------

    private List<TacheRacineStatsDTO> statsParRacine = new ArrayList<>();

    // =========================================================================
    // Sous-DTO : tâche urgente enrichie du chemin (racine > parent > tâche)
    // =========================================================================

    @Getter @Setter @NoArgsConstructor
    public static class TacheUrgenceDTO {
        private Long   id;
        private String nom;
        private String statutKanban;
        private String dateDebutStr;
        private String dateFinStr;
        /** Chemin lisible : "Projet A > Lot 1 > Tâche X" */
        private String chemin;
    }

    // =========================================================================
    // Sous-DTO : stats par tâche racine (pour la barre de progression)
    // =========================================================================

    @Getter @Setter @NoArgsConstructor
    public static class TacheRacineStatsDTO {
        private Long   id;
        private String nom;
        private int    total;
        private int    backlog;
        private int    enCours;
        private int    termine;
        private int    pourcentageTermine;
    }
}
