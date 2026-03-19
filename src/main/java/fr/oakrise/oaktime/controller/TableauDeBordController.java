package fr.oakrise.oaktime.controller;

import fr.oakrise.oaktime.dto.TableauDeBordGeneralDTO;
import fr.oakrise.oaktime.service.TableauDeBordGeneralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller pour le tableau de bord général.
 * Route : GET /  → page d'accueil de l'application
 */
@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class TableauDeBordController {

    private final TableauDeBordGeneralService tableauDeBordGeneralService;

    @GetMapping
    public String index(Model model) {
        log.info("Affichage du tableau de bord général");
        TableauDeBordGeneralDTO tdb = tableauDeBordGeneralService.calculer();
        model.addAttribute("tdb", tdb);
        return "dashboard/index";
    }
}
