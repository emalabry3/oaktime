package fr.oakrise.oaktime.service;

import fr.oakrise.oaktime.dto.TacheDTO;
import fr.oakrise.oaktime.entity.StatutKanban;

import java.util.List;

public interface TacheService {

    // --- Lecture ---
    List<TacheDTO> listerTachesRacines();
    TacheDTO trouverParId(Long id);
    TacheDTO trouverParIdAvecSousTaches(Long id);
    List<TacheDTO> rechercherParNom(String nom);

    // --- Écriture ---
    TacheDTO creer(TacheDTO dto);
    TacheDTO creerSousTache(TacheDTO dto, Long parentId);
    TacheDTO modifier(Long id, TacheDTO dto);
    void supprimer(Long id);

    // --- Prédécesseurs ---
    List<TacheDTO> listerSoeursEligibles(Long tacheId);
    void ajouterPredecesseur(Long tacheId, Long predecesseurId);
    void supprimerPredecesseur(Long tacheId, Long predecesseurId);

    /**
     * Change le statut Kanban avec application des règles R3, R4 et propagation.
     * Retourne les noms des tâches ascendantes auto-passées TERMINE.
     */
    List<String> changerStatut(Long id, StatutKanban statut);

    /**
     * R6 — Décale les dates de début des successeurs pour respecter R5.
     * Appelé uniquement si l'utilisateur a confirmé le décalage.
     *
     * @param tacheId id de la tâche dont la dateFin vient d'être modifiée
     */
    void decalerSuccesseurs(Long tacheId);

    /**
     * Vérifie si la modification de la dateFin de tacheId violerait R5
     * pour au moins un successeur. Retourne la liste des successeurs impactés (noms).
     */
    List<String> verifierImpactSuccesseurs(Long tacheId, java.time.LocalDate nouvelleDataFin);

    fr.oakrise.oaktime.dto.TableauDeBordDTO calculerTableauDeBord(Long tacheId);
}