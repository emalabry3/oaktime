package fr.oakrise.oaktime.service.impl;

import fr.oakrise.oaktime.dto.TraceTacheDTO;
import fr.oakrise.oaktime.entity.Tache;
import fr.oakrise.oaktime.entity.TraceTache;
import fr.oakrise.oaktime.entity.TypeTrace;
import fr.oakrise.oaktime.exception.TacheNotFoundException;
import fr.oakrise.oaktime.exception.TacheValidationException;
import fr.oakrise.oaktime.repository.TacheRepository;
import fr.oakrise.oaktime.repository.TraceTacheRepository;
import fr.oakrise.oaktime.service.TraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraceServiceImpl implements TraceService {

    private final TraceTacheRepository traceTacheRepository;
    private final TacheRepository tacheRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void tracer(Long tacheId, String tacheNom, TypeTrace type, String message) {
        // Créer la trace native sur la tâche concernée
        TraceTache trace = TraceTache.creer(tacheId, type, message);
        trace.setTacheOrigineNom(tacheNom);
        trace.setDateHeure(java.time.LocalDateTime.now());
        traceTacheRepository.save(trace);

        // Propager aux ancêtres
        Tache tache = tacheRepository.findById(tacheId).orElse(null);
        if (tache == null) return;

        Tache ancetre = tache.getParent();
        while (ancetre != null) {
            TraceTache remontee = TraceTache.remonter(ancetre.getId(), trace);
            traceTacheRepository.save(remontee);
            ancetre = ancetre.getParent();
        }

        log.debug("Trace [{}] enregistrée pour tâche id={} : {}", type, tacheId, message);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<TraceTacheDTO> listerParTache(Long tacheId) {
        List<fr.oakrise.oaktime.entity.TraceTache> result =
            traceTacheRepository.findByTacheIdOrderByDateHeureDesc(tacheId);
        if (result == null) return java.util.Collections.emptyList();
        return result.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void supprimerTrace(Long traceId) {
        TraceTache trace = traceTacheRepository.findById(traceId)
                .orElseThrow(() -> new TacheNotFoundException(traceId));
        if (trace.getTypeTrace() != TypeTrace.MANUEL) {
            throw new TacheValidationException("Seules les traces manuelles peuvent être supprimées.");
        }
        traceTacheRepository.deleteById(traceId);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private TraceTacheDTO toDTO(TraceTache t) {
        TraceTacheDTO dto = new TraceTacheDTO();
        dto.setId(t.getId());
        dto.setTacheId(t.getTacheId());
        dto.setTacheOrigineId(t.getTacheOrigineId());
        dto.setTacheOrigineNom(t.getTacheOrigineNom());
        dto.setTypeTrace(t.getTypeTrace());
        dto.setMessage(t.getMessage());
        dto.setDateHeure(t.getDateHeure());
        dto.setDateHeureAffichage(t.getDateHeure() != null ? t.getDateHeure().format(FMT) : "—");
        return dto;
    }
}