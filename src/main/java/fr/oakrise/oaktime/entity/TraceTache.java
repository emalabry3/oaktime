package fr.oakrise.oaktime.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Trace d'une action sur une tâche.
 *
 * Chaque trace est attachée à une tâche via tacheId.
 * Lorsqu'une action se produit sur une sous-tâche, la trace est également
 * dupliquée chez tous ses ancêtres avec tacheOrigineId renseigné.
 */
@Entity
@Table(name = "trace_tache")
@Getter
@Setter
@NoArgsConstructor
public class TraceTache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /** Identifiant de la tâche à laquelle cette trace est rattachée. */
    @Column(name = "tache_id", nullable = false)
    private Long tacheId;

    /**
     * Identifiant de la tâche sur laquelle l'action a réellement eu lieu.
     * Null si la trace est native (action directe sur cette tâche).
     * Renseigné si la trace est remontée depuis une sous-tâche.
     */
    @Column(name = "tache_origine_id")
    private Long tacheOrigineId;

    /** Nom de la tâche d'origine au moment de la trace (dénormalisé pour l'affichage). */
    @Column(name = "tache_origine_nom", length = 255)
    private String tacheOrigineNom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_trace", nullable = false, length = 30)
    private TypeTrace typeTrace;

    @NotBlank
    @Size(max = 2000)
    @Column(name = "message", nullable = false, length = 2000)
    private String message;

    @Column(name = "date_heure", nullable = false, updatable = false)
    private LocalDateTime dateHeure;

    @PrePersist
    protected void onPrePersist() {
        this.dateHeure = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Constructeurs utilitaires
    // -------------------------------------------------------------------------

    /** Trace native (action directe sur la tâche). */
    public static TraceTache creer(Long tacheId, TypeTrace type, String message) {
        TraceTache t = new TraceTache();
        t.tacheId   = tacheId;
        t.typeTrace = type;
        t.message   = message;
        return t;
    }

    /** Trace remontée depuis une sous-tâche vers un ancêtre. */
    public static TraceTache remonter(Long tacheAncetreId, TraceTache source) {
        TraceTache t = new TraceTache();
        t.tacheId          = tacheAncetreId;
        t.tacheOrigineId   = source.tacheOrigineId != null ? source.tacheOrigineId : source.tacheId;
        t.tacheOrigineNom  = source.tacheOrigineNom;
        t.typeTrace        = source.typeTrace;
        t.message          = source.message;
        t.dateHeure        = source.dateHeure;
        return t;
    }
}