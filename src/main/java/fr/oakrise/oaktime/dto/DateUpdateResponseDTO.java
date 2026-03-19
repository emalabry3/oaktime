package fr.oakrise.oaktime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO de réponse pour l'endpoint POST /taches/{id}/dates.
 * Remplace la construction manuelle de JSON (cas R6 notamment).
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DateUpdateResponseDTO {

    /**
     * Indique si la réponse est un avertissement R6
     * (successeurs dont la dateDebut serait violée).
     */
    private Boolean r6;

    /**
     * Noms des successeurs impactés par la violation R6,
     * séparés par des virgules.
     */
    private String impactes;

    /** Message d'erreur en cas de violation d'une autre règle. */
    private String erreur;

    public static DateUpdateResponseDTO ok() {
        return new DateUpdateResponseDTO();
    }

    public static DateUpdateResponseDTO conflitR6(List<String> nomImpactes) {
        DateUpdateResponseDTO dto = new DateUpdateResponseDTO();
        dto.r6 = true;
        dto.impactes = String.join(", ", nomImpactes);
        return dto;
    }

    public static DateUpdateResponseDTO erreur(String message) {
        DateUpdateResponseDTO dto = new DateUpdateResponseDTO();
        dto.erreur = message;
        return dto;
    }
}
