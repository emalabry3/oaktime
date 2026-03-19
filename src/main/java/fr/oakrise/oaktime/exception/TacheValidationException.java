package fr.oakrise.oaktime.exception;

/**
 * Exception levée lorsqu'une règle de gestion métier est violée sur une tâche.
 *
 * Distincte de TacheNotFoundException (ressource absente) et des erreurs
 * de validation Bean Validation (contraintes techniques sur les champs).
 * Cette exception porte des violations de règles métier explicites.
 */
public class TacheValidationException extends RuntimeException {

    /**
     * Construit l'exception avec un message décrivant la règle violée.
     *
     * @param message description de la violation
     */
    public TacheValidationException(String message) {
        super(message);
    }
}