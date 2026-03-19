package fr.oakrise.oaktime.service.component;

import fr.oakrise.oaktime.dto.TacheDTO;
import fr.oakrise.oaktime.entity.StatutKanban;

/**
 * Composant de validation des règles de gestion des tâches.
 */
public interface TacheValidationService {

    /**
     * Valide R1 et R2 avant création ou modification.
     * R1 : dateFin >= dateDebut.
     * R2 : dates dans l'intervalle du parent.
     */
    void valider(TacheDTO dto, Long parentId);

    /**
     * Valide R7/R8 : les dates ne peuvent plus être modifiées selon le statut.
     * R7 : EN_COURS → dateDebut non modifiable.
     * R8 : TERMINE  → dateDebut et dateFin non modifiables.
     *
     * @param statutActuel statut actuel de la tâche en base
     * @param dto          les nouvelles valeurs proposées
     * @param actuelle     les valeurs actuelles en base (pour comparer)
     */
    void validerModificationDates(StatutKanban statutActuel, TacheDTO dto, TacheDTO actuelle);
}