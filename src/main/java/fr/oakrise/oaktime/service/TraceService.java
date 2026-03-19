package fr.oakrise.oaktime.service;

import fr.oakrise.oaktime.dto.TraceTacheDTO;
import fr.oakrise.oaktime.entity.TypeTrace;

import java.util.List;

/**
 * Service de gestion du journal de traçage des tâches.
 */
public interface TraceService {

    /**
     * Enregistre une trace sur la tâche et la propage à tous ses ancêtres.
     *
     * @param tacheId     identifiant de la tâche concernée
     * @param tacheNom    nom de la tâche (pour la dénormalisation chez les ancêtres)
     * @param type        type d'action
     * @param message     description de l'action
     */
    void tracer(Long tacheId, String tacheNom, TypeTrace type, String message);

    /**
     * Retourne toutes les traces d'une tâche, du plus récent au plus ancien.
     */
    List<TraceTacheDTO> listerParTache(Long tacheId);

    /**
     * Supprime une trace manuelle. Lève une exception si la trace n'est pas manuelle.
     */
    void supprimerTrace(Long traceId);
}