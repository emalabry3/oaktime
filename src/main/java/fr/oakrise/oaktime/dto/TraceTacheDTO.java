package fr.oakrise.oaktime.dto;

import fr.oakrise.oaktime.entity.TypeTrace;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TraceTacheDTO {

    private Long id;
    private Long tacheId;
    private Long tacheOrigineId;
    private String tacheOrigineNom;
    private TypeTrace typeTrace;
    private String message;
    private LocalDateTime dateHeure;
    private String dateHeureAffichage; // dd/MM/yyyy HH:mm — formaté côté controller

    /** True si la trace est remontée depuis une sous-tâche. */
    public boolean isRemontee() {
        return tacheOrigineId != null;
    }

    /** True si la trace est manuelle (saisie utilisateur). */
    public boolean isManuelle() {
        return TypeTrace.MANUEL == typeTrace;
    }
}