package fr.oakrise.oaktime.repository;

import fr.oakrise.oaktime.entity.Tache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour l'entité {@link Tache}.
 *
 * Fournit les opérations CRUD standard héritées de JpaRepository,
 * ainsi que des requêtes métier spécifiques à la gestion des tâches.
 */
@Repository
public interface TacheRepository extends JpaRepository<Tache, Long> {

    // -------------------------------------------------------------------------
    // Requêtes sur la hiérarchie
    // -------------------------------------------------------------------------

    /**
     * Récupère toutes les tâches racines (sans parent), triées par date de création.
     * Correspond au premier niveau de l'arborescence.
     *
     * @return la liste des tâches racines
     */
    List<Tache> findByParentIsNullOrderByDateCreationAsc();

    /**
     * Récupère les sous-tâches directes d'une tâche parente donnée,
     * triées par date de création.
     *
     * @param parentId l'identifiant de la tâche parente
     * @return la liste des sous-tâches directes
     */
    List<Tache> findByParentIdOrderByDateCreationAsc(Long parentId);

    /**
     * Charge une tâche avec ses prédécesseurs et ses successeurs en deux requêtes distinctes.
     */
    @Query("SELECT t FROM Tache t LEFT JOIN FETCH t.predecesseurs WHERE t.id = :id")
    Optional<Tache> findByIdWithPredecesseurs(@Param("id") Long id);

    @Query("SELECT t FROM Tache t LEFT JOIN FETCH t.successeurs WHERE t.id = :id")
    Optional<Tache> findByIdWithSuccesseurs(@Param("id") Long id);

    /**
     * Charge une tâche avec ses sous-tâches directes en une seule requête,
     * en évitant le problème N+1 grâce au JOIN FETCH.
     *
     * @param id l'identifiant de la tâche
     * @return la tâche avec ses sous-tâches chargées, ou vide si non trouvée
     */
    @Query("SELECT t FROM Tache t LEFT JOIN FETCH t.sousTaches WHERE t.id = :id")
    Optional<Tache> findByIdWithSousTaches(@Param("id") Long id);

    /**
     * Vérifie si une tâche possède au moins une sous-tâche.
     * Utile pour afficher ou masquer les contrôles d'arborescence dans l'UI.
     *
     * @param parentId l'identifiant de la tâche à vérifier
     * @return true si la tâche a au moins une sous-tâche
     */
    boolean existsByParentId(Long parentId);

    /**
     * Compte le nombre de sous-tâches directes d'une tâche.
     *
     * @param parentId l'identifiant de la tâche parente
     * @return le nombre de sous-tâches directes
     */
    long countByParentId(Long parentId);

    // -------------------------------------------------------------------------
    // Requêtes métier
    // -------------------------------------------------------------------------

    /**
     * Recherche des tâches racines dont le nom contient le texte donné (insensible à la casse).
     * Utilisé pour la fonctionnalité de recherche dans la liste principale.
     *
     * @param nom le texte à rechercher dans le nom
     * @return la liste des tâches correspondantes
     */
    List<Tache> findByParentIsNullAndNomContainingIgnoreCase(String nom);

    /**
     * Récupère les tâches en retard : date de fin effective dépassée et tâche non terminée.
     * Une tâche est considérée en retard si sa date de fin corrigée (ou initiale si null)
     * est antérieure à la date du jour.
     *
     * Note : cette requête ne tient pas compte d'un éventuel statut "terminé"
     * qui sera ajouté dans une prochaine itération.
     *
     * @param dateJour la date du jour à comparer
     * @return la liste des tâches en retard
     */
    @Query("""
            SELECT t FROM Tache t
            WHERE t.dateFin IS NOT NULL AND t.dateFin < :dateJour
            ORDER BY t.dateFin ASC
            """)
    List<Tache> findTachesEnRetard(@Param("dateJour") LocalDate dateJour);

    /**
     * Récupère les tâches dont la dateFin se situe dans un intervalle donné.
     */
    @Query("""
            SELECT t FROM Tache t
            WHERE t.dateFin IS NOT NULL AND t.dateFin BETWEEN :debut AND :fin
            ORDER BY t.dateFin ASC
            """)
    List<Tache> findByDateFinBetween(
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin
    );
}