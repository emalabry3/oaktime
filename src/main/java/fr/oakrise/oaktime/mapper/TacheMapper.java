package fr.oakrise.oaktime.mapper;

import fr.oakrise.oaktime.dto.TacheDTO;
import fr.oakrise.oaktime.entity.Tache;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TacheMapper {

    public TacheDTO toDTO(Tache tache) {
        if (tache == null) return null;

        TacheDTO dto = new TacheDTO();
        dto.setId(tache.getId());
        dto.setNom(tache.getNom());
        dto.setDescription(tache.getDescription());
        dto.setDateCreation(tache.getDateCreation());
        dto.setDateDebut(tache.getDateDebut());
        dto.setDateFin(tache.getDateFin());
        dto.setDuree(tache.getDuree());
        dto.setRacine(tache.estRacine());
        dto.setStatutKanban(tache.getStatutKanban());

        if (tache.getParent() != null) {
            dto.setParentId(tache.getParent().getId());
            dto.setParentNom(tache.getParent().getNom());
        }

        dto.setNombreSousTaches(tache.getSousTaches().size());

        // Prédécesseurs — mapping léger
        dto.setPredecesseurs(tache.getPredecesseurs().stream()
                .map(p -> {
                    TacheDTO pd = new TacheDTO();
                    pd.setId(p.getId());
                    pd.setNom(p.getNom());
                    pd.setDateFin(p.getDateFin());
                    pd.setStatutKanban(p.getStatutKanban());
                    return pd;
                })
                .collect(Collectors.toList()));

        // Successeurs — mapping léger
        dto.setSuccesseurs(tache.getSuccesseurs().stream()
                .map(s -> {
                    TacheDTO sd = new TacheDTO();
                    sd.setId(s.getId());
                    sd.setNom(s.getNom());
                    sd.setDateDebut(s.getDateDebut());
                    sd.setDateFin(s.getDateFin());
                    sd.setStatutKanban(s.getStatutKanban());
                    return sd;
                })
                .collect(Collectors.toList()));

        return dto;
    }

    public TacheDTO toDTOAvecSousTaches(Tache tache) {
        if (tache == null) return null;
        TacheDTO dto = toDTO(tache);
        dto.setSousTaches(tache.getSousTaches().stream()
                .map(this::toDTOAvecSousTaches)
                .collect(Collectors.toList()));
        return dto;
    }

    public List<TacheDTO> toDTOList(List<Tache> taches) {
        if (taches == null) return Collections.emptyList();
        return taches.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public Tache toEntity(TacheDTO dto) {
        if (dto == null) return null;
        return mergeToEntity(dto, new Tache());
    }

    /**
     * Merge : ne touche pas dateDebut/dateFin si la tâche est EN_COURS (R7) ou TERMINE (R8).
     * Ces contrôles sont faits en amont dans le service ; ici on applique sans condition.
     */
    public Tache mergeToEntity(TacheDTO dto, Tache tache) {
        tache.setNom(dto.getNom());
        tache.setDescription(dto.getDescription());
        tache.setDuree(dto.getDuree());

        // dateDebut : appliquée seulement si le service l'a autorisée (R7, R8)
        tache.setDateDebut(dto.getDateDebut());
        tache.setDateFin(dto.getDateFin());

        if (dto.getStatutKanban() != null) {
            tache.setStatutKanban(dto.getStatutKanban());
        }
        return tache;
    }
}