package fr.oakrise.oaktime.dto;

import fr.oakrise.oaktime.entity.StatutKanban;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TacheDTO {

    private Long id;

    @NotBlank(message = "Le nom de la tâche est obligatoire")
    @Size(max = 255, message = "Le nom ne peut pas dépasser 255 caractères")
    private String nom;

    @Size(max = 4000, message = "La description ne peut pas dépasser 4000 caractères")
    private String description;

    private LocalDateTime dateCreation;

    /** Date de début planifiée / effective. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;

    /** Date de fin planifiée / effective. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;

    /** Durée estimée en jours. */
    @Min(value = 0, message = "La durée ne peut pas être négative")
    private Integer duree;

    private Long parentId;
    private String parentNom;

    private List<TacheDTO> predecesseurs = new ArrayList<>();
    private List<TacheDTO> successeurs   = new ArrayList<>();
    private List<TacheDTO> sousTaches    = new ArrayList<>();

    private long nombreSousTaches;
    private boolean racine;

    private StatutKanban statutKanban = StatutKanban.BACKLOG;

    // Champs calculés pour l'affichage (non persistés)
    private String dateDebutStr;       // yyyy-MM-dd
    private String dateFinStr;         // yyyy-MM-dd
    private String dateDebutAffichage; // dd/MM/yy
    private String dateFinAffichage;   // dd/MM/yy
}