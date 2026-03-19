package fr.oakrise.oaktime.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import fr.oakrise.oaktime.exception.TacheValidationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Gestionnaire global des exceptions pour les vues Thymeleaf.
 *
 * Intercepte les exceptions non gérées localement dans les controllers
 * et les traduit en pages d'erreur lisibles pour l'utilisateur.
 *
 * Note : lors d'une migration vers REST, ce composant sera complété
 * ou remplacé par un handler dédié retournant des réponses JSON.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les violations de règles métier (dates incohérentes, contraintes hiérarchiques…).
     * Redirige vers la page précédente avec un message d'erreur flash.
     *
     * @param ex      l'exception levée
     * @param request la requête HTTP pour construire la redirection
     * @param model   le modèle Thymeleaf
     * @return la vue "erreur/validation"
     */
    @ExceptionHandler(TacheValidationException.class)
    public String handleTacheValidation(
            TacheValidationException ex,
            Model model) {
        log.warn("Violation de règle métier : {}", ex.getMessage());
        model.addAttribute("messageErreur", ex.getMessage());
        return "erreur/validation";
    }

    /**
     * Gère les cas où une tâche demandée est introuvable en base.
     * Affiche une page d'erreur 404 avec un message explicite.
     *
     * @param ex    l'exception levée
     * @param model le modèle Thymeleaf pour passer le message à la vue
     * @return la vue "erreur/404"
     */
    @ExceptionHandler(TacheNotFoundException.class)
    public String handleTacheNotFound(TacheNotFoundException ex, Model model) {
        log.warn("Tâche introuvable : {}", ex.getMessage());
        model.addAttribute("messageErreur", ex.getMessage());
        return "erreur/404";
    }

    /**
     * Gère toutes les exceptions non anticipées.
     * Affiche une page d'erreur générique pour éviter d'exposer
     * des détails techniques à l'utilisateur.
     *
     * @param ex    l'exception levée
     * @param model le modèle Thymeleaf
     * @return la vue "erreur/500"
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        log.error("Erreur inattendue : {}", ex.getMessage(), ex);
        model.addAttribute("messageErreur", "Une erreur inattendue s'est produite.");
        return "erreur/500";
    }
}