package fr.oakrise.oaktime.service.component;

import fr.oakrise.oaktime.dto.TacheDTO;
import fr.oakrise.oaktime.entity.StatutKanban;
import fr.oakrise.oaktime.entity.Tache;
import fr.oakrise.oaktime.exception.TacheNotFoundException;
import fr.oakrise.oaktime.exception.TacheValidationException;
import fr.oakrise.oaktime.repository.TacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TacheValidationServiceImpl implements TacheValidationService {

    private final TacheRepository tacheRepository;

    @Override
    public void valider(TacheDTO dto, Long parentId) {
        log.debug("Validation règles R1/R2 pour '{}'", dto.getNom());
        validerR1(dto);
        if (parentId != null) {
            validerR2(dto, parentId);
        }
    }

    @Override
    public void validerModificationDates(StatutKanban statutActuel, TacheDTO dto, TacheDTO actuelle) {
        // R8 : TERMINE — dateDebut et dateFin immuables
        if (statutActuel == StatutKanban.TERMINE) {
            if (!java.util.Objects.equals(dto.getDateDebut(), actuelle.getDateDebut())) {
                throw new TacheValidationException(
                    "R8 — La date de début ne peut plus être modifiée : la tâche est terminée.");
            }
            if (!java.util.Objects.equals(dto.getDateFin(), actuelle.getDateFin())) {
                throw new TacheValidationException(
                    "R8 — La date de fin ne peut plus être modifiée : la tâche est terminée.");
            }
        }
        // R7 : EN_COURS — dateDebut immuable
        if (statutActuel == StatutKanban.EN_COURS) {
            if (!java.util.Objects.equals(dto.getDateDebut(), actuelle.getDateDebut())) {
                throw new TacheValidationException(
                    "R7 — La date de début ne peut plus être modifiée : la tâche est en cours.");
            }
        }
    }

    // -------------------------------------------------------------------------

    /** R1 : dateFin >= dateDebut */
    private void validerR1(TacheDTO dto) {
        LocalDate debut = dto.getDateDebut();
        LocalDate fin   = dto.getDateFin();
        if (debut != null && fin != null && fin.isBefore(debut)) {
            throw new TacheValidationException(
                "R1 — La date de fin (" + fin + ") ne peut pas être antérieure à la date de début (" + debut + ").");
        }
    }

    /** R2 : dates sous-tâche dans l'intervalle [dateDebut, dateFin] du parent */
    private void validerR2(TacheDTO dto, Long parentId) {
        Tache parent = tacheRepository.findById(parentId)
                .orElseThrow(() -> new TacheNotFoundException(parentId));

        LocalDate debutParent = parent.getDateDebut();
        LocalDate finParent   = parent.getDateFin();
        LocalDate debutST     = dto.getDateDebut();
        LocalDate finST       = dto.getDateFin();

        if (debutST != null && debutParent != null && debutST.isBefore(debutParent)) {
            throw new TacheValidationException(
                "R2 — La date de début (" + debutST +
                ") ne peut pas être antérieure à celle du parent (" + debutParent + ").");
        }
        if (finST != null && finParent != null && finST.isAfter(finParent)) {
            throw new TacheValidationException(
                "R2 — La date de fin (" + finST +
                ") ne peut pas dépasser celle du parent (" + finParent + ").");
        }
        log.debug("R2 OK pour '{}' (parent id={})", dto.getNom(), parentId);
    }
}