package fr.oakrise.oaktime.repository;

import fr.oakrise.oaktime.entity.TraceTache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TraceTacheRepository extends JpaRepository<TraceTache, Long> {

    /** Toutes les traces d'une tâche, du plus récent au plus ancien. */
    List<TraceTache> findByTacheIdOrderByDateHeureDesc(Long tacheId);

    /** Supprimer toutes les traces d'une tâche (utilisé lors de la suppression). */
    void deleteByTacheId(Long tacheId);
}