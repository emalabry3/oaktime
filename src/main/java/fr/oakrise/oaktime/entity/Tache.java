package fr.oakrise.oaktime.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant une tâche dans le système de gestion du temps.
 *
 * Règles de dates :
 *   R1 : dateFin ne peut pas être avant dateDebut.
 *   R2 : les dates d'une sous-tâche doivent rester dans l'intervalle [dateDebut, dateFin] du parent.
 *   R3 : passage EN_COURS → dateDebut = aujourd'hui (si non déjà renseignée).
 *   R4 : passage TERMINE  → dateFin   = aujourd'hui.
 *   R5 : dateDebut >= max(dateFin des prédécesseurs).
 *   R6 : si modification d'un prédécesseur viole R5 chez un successeur, demander à l'utilisateur.
 *   R7 : EN_COURS → dateDebut non modifiable.
 *   R8 : TERMINE  → dateDebut et dateFin non modifiables.
 */
@Entity
@Table(name = "tache")
@Getter
@Setter
@NoArgsConstructor
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Le nom de la tâche est obligatoire")
    @Size(max = 255, message = "Le nom ne peut pas dépasser 255 caractères")
    @Column(name = "nom", nullable = false, length = 255)
    private String nom;

    @Size(max = 4000, message = "La description ne peut pas dépasser 4000 caractères")
    @Column(name = "description", length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_kanban", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'BACKLOG'")
    private StatutKanban statutKanban = StatutKanban.BACKLOG;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Date de début planifiée ou effective.
     * Automatiquement fixée à aujourd'hui lors du passage EN_COURS (R3).
     * Non modifiable une fois EN_COURS ou TERMINE (R7, R8).
     */
    @Column(name = "date_debut")
    private LocalDate dateDebut;

    /**
     * Date de fin planifiée ou effective.
     * Automatiquement fixée à aujourd'hui lors du passage TERMINE (R4).
     * Non modifiable une fois TERMINE (R8).
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /**
     * Durée estimée en jours entiers. Optionnelle.
     */
    @Min(value = 0, message = "La durée ne peut pas être négative")
    @Column(name = "duree")
    private Integer duree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Tache parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateCreation ASC")
    private List<Tache> sousTaches = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tache_predecesseur",
        joinColumns = @JoinColumn(name = "tache_id"),
        inverseJoinColumns = @JoinColumn(name = "predecesseur_id")
    )
    @OrderBy("dateFin ASC NULLS LAST")
    private List<Tache> predecesseurs = new ArrayList<>();

    @ManyToMany(mappedBy = "predecesseurs", fetch = FetchType.LAZY)
    private List<Tache> successeurs = new ArrayList<>();

    @PrePersist
    protected void onPrePersist() {
        this.dateCreation = LocalDateTime.now();
        if (this.statutKanban == null) this.statutKanban = StatutKanban.BACKLOG;
    }

    public void ajouterPredecesseur(Tache predecesseur) {
        this.predecesseurs.add(predecesseur);
        predecesseur.getSuccesseurs().add(this);
    }

    public void retirerPredecesseur(Tache predecesseur) {
        this.predecesseurs.remove(predecesseur);
        predecesseur.getSuccesseurs().remove(this);
    }

    public void ajouterSousTache(Tache sousTache) {
        sousTache.setParent(this);
        this.sousTaches.add(sousTache);
    }

    public void retirerSousTache(Tache sousTache) {
        sousTache.setParent(null);
        this.sousTaches.remove(sousTache);
    }

    public boolean estRacine() {
        return this.parent == null;
    }

    public boolean aSousTaches() {
        return !this.sousTaches.isEmpty();
    }

    /**
     * Date de fin maximale parmi les prédécesseurs (R5).
     */
    public LocalDate dateFinMaxPredecesseurs() {
        return this.predecesseurs.stream()
                .map(Tache::getDateFin)
                .filter(java.util.Objects::nonNull)
                .max(java.util.Comparator.naturalOrder())
                .orElse(null);
    }
}