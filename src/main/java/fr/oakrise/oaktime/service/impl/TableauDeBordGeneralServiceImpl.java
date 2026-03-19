package fr.oakrise.oaktime.service.impl;

import fr.oakrise.oaktime.dto.TableauDeBordGeneralDTO;
import fr.oakrise.oaktime.dto.TableauDeBordGeneralDTO.TacheUrgenceDTO;
import fr.oakrise.oaktime.dto.TableauDeBordGeneralDTO.TacheRacineStatsDTO;
import fr.oakrise.oaktime.entity.StatutKanban;
import fr.oakrise.oaktime.entity.Tache;
import fr.oakrise.oaktime.repository.TacheRepository;
import fr.oakrise.oaktime.service.TableauDeBordGeneralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TableauDeBordGeneralServiceImpl implements TableauDeBordGeneralService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yy");

    private final TacheRepository tacheRepository;

    @Override
    @Transactional(readOnly = true)
    public TableauDeBordGeneralDTO calculer() {
        log.info("Calcul du tableau de bord général");

        List<Tache> racines  = tacheRepository.findByParentIsNullOrderByDateCreationAsc();
        LocalDate   today    = LocalDate.now();
        LocalDate   dans7J   = today.plusDays(7);

        TableauDeBordGeneralDTO tdb = new TableauDeBordGeneralDTO();

        int totalBacklog = 0, totalEnCours = 0, totalTermine = 0;
        List<TacheUrgenceDTO> retards    = new ArrayList<>();
        List<TacheUrgenceDTO> echeances  = new ArrayList<>();
        List<TacheUrgenceDTO> bloquees   = new ArrayList<>();
        int totalTaches = 0;

        for (Tache racine : racines) {

            // Collecter racine + tous les descendants avec leur chemin
            List<Tache>  descendants = new ArrayList<>();
            List<String> chemins     = new ArrayList<>();
            collecterAvecChemin(racine, racine.getNom(), descendants, chemins);

            // Stats par racine (uniquement les descendants, pas la racine elle-même)
            int rb = 0, re = 0, rt = 0;
            for (Tache t : descendants) {
                switch (t.getStatutKanban()) {
                    case BACKLOG   -> rb++;
                    case EN_COURS  -> re++;
                    case TERMINE   -> rt++;
                }
            }
            TacheRacineStatsDTO stats = new TacheRacineStatsDTO();
            stats.setId(racine.getId());
            stats.setNom(racine.getNom());
            stats.setBacklog(rb);
            stats.setEnCours(re);
            stats.setTermine(rt);
            stats.setTotal(descendants.size());
            stats.setPourcentageTermine(descendants.isEmpty() ? 0
                : (int) Math.round(100.0 * rt / descendants.size()));
            tdb.getStatsParRacine().add(stats);

            // Compteurs globaux + listes urgentes
            for (int i = 0; i < descendants.size(); i++) {
                Tache  t      = descendants.get(i);
                String chemin = chemins.get(i);
                totalTaches++;

                switch (t.getStatutKanban()) {
                    case BACKLOG   -> totalBacklog++;
                    case EN_COURS  -> totalEnCours++;
                    case TERMINE   -> totalTermine++;
                }

                // Retard démarrage
                if (t.getStatutKanban() == StatutKanban.BACKLOG
                        && t.getDateDebut() != null
                        && t.getDateDebut().isBefore(today)) {
                    retards.add(toUrgence(t, chemin));
                }

                // Échéance proche
                if (t.getStatutKanban() == StatutKanban.EN_COURS
                        && t.getDateFin() != null
                        && !t.getDateFin().isAfter(dans7J)) {
                    echeances.add(toUrgence(t, chemin));
                }

                // Bloquée
                if (t.getStatutKanban() != StatutKanban.TERMINE) {
                    boolean bloque = t.getPredecesseurs().stream()
                            .anyMatch(p -> p.getStatutKanban() != StatutKanban.TERMINE);
                    if (bloque) bloquees.add(toUrgence(t, chemin));
                }
            }
        }

        tdb.setTotalTaches(totalTaches);
        tdb.setTotalBacklog(totalBacklog);
        tdb.setTotalEnCours(totalEnCours);
        tdb.setTotalTermine(totalTermine);
        tdb.setPourcentageTermine(totalTaches == 0 ? 0
            : (int) Math.round(100.0 * totalTermine / totalTaches));

        retards.sort(Comparator.comparing(TacheUrgenceDTO::getDateDebutStr));
        tdb.setTachesEnRetardDemarrage(retards);

        echeances.sort(Comparator.comparing(TacheUrgenceDTO::getDateFinStr));
        tdb.setTachesEcheanceProche(echeances);

        tdb.setTachesBloquees(bloquees);

        return tdb;
    }

    // -------------------------------------------------------------------------
    // Collecte récursive avec construction du chemin
    // -------------------------------------------------------------------------

    private void collecterAvecChemin(Tache tache, String cheminParent,
                                     List<Tache> resultat, List<String> chemins) {
        for (Tache enfant : tache.getSousTaches()) {
            String cheminEnfant = cheminParent + " > " + enfant.getNom();
            resultat.add(enfant);
            chemins.add(cheminEnfant);
            collecterAvecChemin(enfant, cheminEnfant, resultat, chemins);
        }
    }

    private TacheUrgenceDTO toUrgence(Tache t, String chemin) {
        TacheUrgenceDTO dto = new TacheUrgenceDTO();
        dto.setId(t.getId());
        dto.setNom(t.getNom());
        dto.setStatutKanban(t.getStatutKanban().name());
        dto.setDateDebutStr(t.getDateDebut() != null ? t.getDateDebut().format(FMT) : "");
        dto.setDateFinStr(t.getDateFin()   != null ? t.getDateFin().format(FMT)   : "");
        dto.setChemin(chemin);
        return dto;
    }
}
