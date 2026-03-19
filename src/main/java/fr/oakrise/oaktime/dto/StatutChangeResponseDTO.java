package fr.oakrise.oaktime.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO de réponse pour l'endpoint POST /taches/{id}/statut.
 * Remplace la construction manuelle de JSON via StringBuilder.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatutChangeResponseDTO {

    /** Liste des noms de tâches parentes automatiquement passées en TERMINE. */
    private List<String> autoTermines;

    /** Message d'erreur en cas de violation de règle métier (ex. R5). */
    private String erreur;

    public static StatutChangeResponseDTO succes(List<String> autoTermines) {
        StatutChangeResponseDTO dto = new StatutChangeResponseDTO();
        dto.autoTermines = autoTermines;
        return dto;
    }

    public static StatutChangeResponseDTO erreur(String message) {
        StatutChangeResponseDTO dto = new StatutChangeResponseDTO();
        dto.erreur = message;
        return dto;
    }
}
