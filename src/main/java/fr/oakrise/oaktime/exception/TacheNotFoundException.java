package fr.oakrise.oaktime.exception;

/**
 * Exception levée lorsqu'une tâche est introuvable en base de données.
 *
 * Étend {@link RuntimeException} pour ne pas contraindre les signatures
 * des méthodes avec un checked exception, tout en restant interceptable
 * par le {@code @ControllerAdvice} global.
 */
public class TacheNotFoundException extends RuntimeException {

    /**
     * Construit l'exception avec un message explicite incluant l'identifiant recherché.
     *
     * @param id l'identifiant de la tâche introuvable
     */
    public TacheNotFoundException(Long id) {
        super("Tâche introuvable avec l'identifiant : " + id);
    }

    /**
     * Construit l'exception avec un message personnalisé.
     *
     * @param message le message d'erreur
     */
    public TacheNotFoundException(String message) {
        super(message);
    }
}