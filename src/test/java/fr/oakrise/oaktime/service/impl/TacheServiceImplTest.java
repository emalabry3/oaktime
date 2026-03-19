package fr.oakrise.oaktime.service.impl;

import fr.oakrise.oaktime.dto.TacheDTO;
import fr.oakrise.oaktime.entity.StatutKanban;
import fr.oakrise.oaktime.entity.Tache;
import fr.oakrise.oaktime.exception.TacheNotFoundException;
import fr.oakrise.oaktime.exception.TacheValidationException;
import fr.oakrise.oaktime.mapper.TacheMapper;
import fr.oakrise.oaktime.repository.TacheRepository;
import fr.oakrise.oaktime.service.TraceService;
import fr.oakrise.oaktime.service.component.TacheValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link TacheServiceImpl}.
 *
 * Stratégie : dépendances mockées via Mockito, focus sur la logique métier.
 * Conventions : "étantDonné_quand_alors".
 *
 * Couverture spécifique au nouveau modèle de dates :
 *   R3 — passage EN_COURS → dateDebut = today
 *   R4 — passage TERMINE  → dateFin   = today
 *   R5 — dateDebut >= max(dateFin prédécesseurs)
 *   R7 — EN_COURS : dateDebut non modifiable
 *   R8 — TERMINE  : dateDebut et dateFin non modifiables
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TacheServiceImpl — Tests unitaires")
class TacheServiceImplTest {

    @Mock private TacheRepository tacheRepository;
    @Mock private TacheMapper tacheMapper;
    @Mock private TacheValidationService tacheValidationService;
    @Mock private TraceService traceService;

    @InjectMocks
    private TacheServiceImpl tacheService;

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private Tache tacheEntite;
    private TacheDTO tacheDTO;

    @BeforeEach
    void setUp() {
        tacheEntite = new Tache();
        tacheEntite.setNom("Développer le module auth");
        tacheEntite.setDescription("Implémenter l'authentification JWT");
        tacheEntite.setStatutKanban(StatutKanban.BACKLOG);
        // Simulation @PrePersist
        try {
            var m = Tache.class.getDeclaredMethod("onPrePersist");
            m.setAccessible(true);
            m.invoke(tacheEntite);
        } catch (Exception ignored) {}

        tacheDTO = new TacheDTO();
        tacheDTO.setId(1L);
        tacheDTO.setNom("Développer le module auth");
        tacheDTO.setDescription("Implémenter l'authentification JWT");
        tacheDTO.setDateCreation(LocalDateTime.now());
        tacheDTO.setStatutKanban(StatutKanban.BACKLOG);
        tacheDTO.setRacine(true);
    }

    // Utilitaire pour stuber findById(1L)
    private void stubFindById1() {
        when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
        when(tacheMapper.toDTO(tacheEntite)).thenReturn(tacheDTO);
    }

    // =========================================================================
    // listerTachesRacines()
    // =========================================================================

    @Nested
    @DisplayName("listerTachesRacines()")
    class ListerTachesRacines {

        @Test
        @DisplayName("Retourne la liste des tâches racines mappées en DTO")
        void doit_retourner_taches_racines() {
            List<Tache> entites = List.of(tacheEntite);
            List<TacheDTO> dtos = List.of(tacheDTO);
            when(tacheRepository.findByParentIsNullOrderByDateCreationAsc()).thenReturn(entites);
            when(tacheMapper.toDTOList(entites)).thenReturn(dtos);

            List<TacheDTO> res = tacheService.listerTachesRacines();

            assertThat(res).hasSize(1);
            assertThat(res.get(0).getNom()).isEqualTo("Développer le module auth");
            verify(tacheRepository).findByParentIsNullOrderByDateCreationAsc();
        }

        @Test
        @DisplayName("Retourne liste vide s'il n'y a aucune tâche racine")
        void doit_retourner_liste_vide_si_aucune_tache() {
            when(tacheRepository.findByParentIsNullOrderByDateCreationAsc()).thenReturn(List.of());
            when(tacheMapper.toDTOList(List.of())).thenReturn(List.of());

            assertThat(tacheService.listerTachesRacines()).isEmpty();
        }
    }

    // =========================================================================
    // trouverParId()
    // =========================================================================

    @Nested
    @DisplayName("trouverParId()")
    class TrouverParId {

        @Test
        @DisplayName("Retourne le DTO quand la tâche existe")
        void doit_retourner_dto_si_tache_existe() {
            stubFindById1();
            assertThat(tacheService.trouverParId(1L).getNom())
                    .isEqualTo("Développer le module auth");
        }

        @Test
        @DisplayName("Lève TacheNotFoundException si tâche introuvable")
        void doit_lever_exception_si_tache_introuvable() {
            when(tacheRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> tacheService.trouverParId(99L))
                    .isInstanceOf(TacheNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================================
    // creer()
    // =========================================================================

    @Nested
    @DisplayName("creer()")
    class Creer {

        @Test
        @DisplayName("Crée une tâche racine, persiste et retourne le DTO avec id")
        void doit_creer_tache_racine() {
            TacheDTO entree = new TacheDTO();
            entree.setNom("Nouvelle tâche");

            Tache entite = new Tache();
            entite.setNom("Nouvelle tâche");

            TacheDTO retour = new TacheDTO();
            retour.setId(42L);
            retour.setNom("Nouvelle tâche");

            when(tacheMapper.toEntity(entree)).thenReturn(entite);
            when(tacheRepository.save(entite)).thenReturn(entite);
            when(tacheMapper.toDTO(entite)).thenReturn(retour);

            TacheDTO res = tacheService.creer(entree);

            assertThat(res.getId()).isEqualTo(42L);
            verify(tacheValidationService).valider(entree, null);
            verify(tacheRepository).save(entite);
        }
    }

    // =========================================================================
    // creerSousTache()
    // =========================================================================

    @Nested
    @DisplayName("creerSousTache()")
    class CreerSousTache {

        @Test
        @DisplayName("Rattache la sous-tâche au parent et persiste via cascade")
        void doit_rattacher_sous_tache_au_parent() {
            Tache parent = new Tache();
            parent.setNom("Parent");
            parent.setStatutKanban(StatutKanban.BACKLOG);

            TacheDTO dtoST = new TacheDTO();
            dtoST.setNom("Sous-tâche");

            Tache entiteST = new Tache();
            entiteST.setNom("Sous-tâche");

            TacheDTO retour = new TacheDTO();
            retour.setNom("Sous-tâche");
            retour.setParentId(1L);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(parent));
            when(tacheMapper.toEntity(dtoST)).thenReturn(entiteST);
            when(tacheRepository.save(parent)).thenReturn(parent);
            when(tacheMapper.toDTO(entiteST)).thenReturn(retour);

            TacheDTO res = tacheService.creerSousTache(dtoST, 1L);

            assertThat(parent.getSousTaches()).contains(entiteST);
            assertThat(entiteST.getParent()).isEqualTo(parent);
            assertThat(res.getParentId()).isEqualTo(1L);
            verify(tacheRepository).save(parent);
        }

        @Test
        @DisplayName("Lève TacheNotFoundException si le parent est introuvable")
        void doit_lever_exception_si_parent_introuvable() {
            when(tacheRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> tacheService.creerSousTache(new TacheDTO(), 99L))
                    .isInstanceOf(TacheNotFoundException.class);
            verify(tacheRepository, never()).save(any());
        }
    }

    // =========================================================================
    // modifier() — R7 et R8 testées via TacheValidationService
    // =========================================================================

    @Nested
    @DisplayName("modifier()")
    class Modifier {

        @Test
        @DisplayName("Modifie une tâche BACKLOG sans contrainte de date")
        void doit_modifier_tache_backlog() {
            TacheDTO modif = new TacheDTO();
            modif.setNom("Nom modifié");
            modif.setStatutKanban(StatutKanban.BACKLOG);

            TacheDTO retour = new TacheDTO();
            retour.setId(1L);
            retour.setNom("Nom modifié");

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheMapper.toDTO(tacheEntite)).thenReturn(tacheDTO); // pour validerModificationDates
            when(tacheMapper.mergeToEntity(modif, tacheEntite)).thenReturn(tacheEntite);
            when(tacheRepository.save(tacheEntite)).thenReturn(tacheEntite);
            when(tacheMapper.toDTO(tacheEntite)).thenReturn(retour);

            TacheDTO res = tacheService.modifier(1L, modif);

            assertThat(res.getNom()).isEqualTo("Nom modifié");
            verify(tacheValidationService).validerModificationDates(eq(StatutKanban.BACKLOG), eq(modif), any());
            verify(tacheValidationService).valider(eq(modif), any());
        }

        @Test
        @DisplayName("R7 — service délègue la vérification à TacheValidationService pour EN_COURS")
        void r7_delegue_verification_statut_en_cours() {
            tacheEntite.setStatutKanban(StatutKanban.EN_COURS);
            tacheEntite.setDateDebut(LocalDate.now().minusDays(3));

            TacheDTO modif = new TacheDTO();
            modif.setNom("Modif");
            modif.setDateDebut(LocalDate.now()); // tentative de modif
            modif.setStatutKanban(StatutKanban.EN_COURS);

            // Simuler que le validateur lève l'exception R7
            doThrow(new TacheValidationException("R7 — La date de début ne peut plus être modifiée"))
                    .when(tacheValidationService).validerModificationDates(eq(StatutKanban.EN_COURS), any(), any());

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheMapper.toDTO(tacheEntite)).thenReturn(tacheDTO);

            assertThatThrownBy(() -> tacheService.modifier(1L, modif))
                    .isInstanceOf(TacheValidationException.class)
                    .hasMessageContaining("R7");

            verify(tacheRepository, never()).save(any());
        }

        @Test
        @DisplayName("R8 — service délègue la vérification à TacheValidationService pour TERMINE")
        void r8_delegue_verification_statut_termine() {
            tacheEntite.setStatutKanban(StatutKanban.TERMINE);
            tacheEntite.setDateDebut(LocalDate.now().minusDays(10));
            tacheEntite.setDateFin(LocalDate.now().minusDays(2));

            TacheDTO modif = new TacheDTO();
            modif.setNom("Modif");
            modif.setDateFin(LocalDate.now()); // tentative de modif
            modif.setStatutKanban(StatutKanban.TERMINE);

            doThrow(new TacheValidationException("R8 — La date de fin ne peut plus être modifiée"))
                    .when(tacheValidationService).validerModificationDates(eq(StatutKanban.TERMINE), any(), any());

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheMapper.toDTO(tacheEntite)).thenReturn(tacheDTO);

            assertThatThrownBy(() -> tacheService.modifier(1L, modif))
                    .isInstanceOf(TacheValidationException.class)
                    .hasMessageContaining("R8");

            verify(tacheRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lève TacheNotFoundException si tâche introuvable")
        void doit_lever_exception_si_tache_introuvable() {
            when(tacheRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> tacheService.modifier(99L, tacheDTO))
                    .isInstanceOf(TacheNotFoundException.class);
        }
    }

    // =========================================================================
    // changerStatut() — R3, R4, R5
    // =========================================================================

    @Nested
    @DisplayName("changerStatut() — R3, R4, R5")
    class ChangerStatut {

        @Test
        @DisplayName("R3 — passage EN_COURS sans dateDebut fixe dateDebut = aujourd'hui")
        void r3_fixe_date_debut_si_nulle_lors_passage_en_cours() {
            tacheEntite.setDateDebut(null);
            tacheEntite.setStatutKanban(StatutKanban.BACKLOG);
            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            tacheService.changerStatut(1L, StatutKanban.EN_COURS);

            assertThat(tacheEntite.getDateDebut()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("R3 — passage EN_COURS avec dateDebut déjà renseignée conserve la date existante")
        void r3_conserve_date_debut_si_deja_renseignee() {
            LocalDate debutExistant = LocalDate.now().minusDays(5);
            tacheEntite.setDateDebut(debutExistant);
            tacheEntite.setStatutKanban(StatutKanban.BACKLOG);
            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            tacheService.changerStatut(1L, StatutKanban.EN_COURS);

            assertThat(tacheEntite.getDateDebut()).isEqualTo(debutExistant);
        }

        @Test
        @DisplayName("R4 — passage TERMINE fixe dateFin = aujourd'hui")
        void r4_fixe_date_fin_lors_passage_termine() {
            tacheEntite.setStatutKanban(StatutKanban.EN_COURS);
            tacheEntite.setDateFin(LocalDate.now().plusDays(10)); // date future prévue
            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            tacheService.changerStatut(1L, StatutKanban.TERMINE);

            assertThat(tacheEntite.getDateFin()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("R4 — passage TERMINE fixe dateFin même si elle était nulle")
        void r4_fixe_date_fin_meme_si_nulle() {
            tacheEntite.setStatutKanban(StatutKanban.EN_COURS);
            tacheEntite.setDateFin(null);
            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            tacheService.changerStatut(1L, StatutKanban.TERMINE);

            assertThat(tacheEntite.getDateFin()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("R5 — lève exception si un prédécesseur n'est pas terminé (passage EN_COURS)")
        void r5_leve_exception_si_predecesseur_non_termine_en_cours() {
            Tache pred = new Tache();
            pred.setNom("Prédécesseur");
            pred.setStatutKanban(StatutKanban.EN_COURS); // pas terminé
            tacheEntite.getPredecesseurs().add(pred);
            tacheEntite.setStatutKanban(StatutKanban.BACKLOG);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));

            assertThatThrownBy(() -> tacheService.changerStatut(1L, StatutKanban.EN_COURS))
                    .isInstanceOf(TacheValidationException.class)
                    .hasMessageContaining("R5");
        }

        @Test
        @DisplayName("R5 — lève exception si un prédécesseur n'est pas terminé (passage TERMINE)")
        void r5_leve_exception_si_predecesseur_non_termine_pour_terminer() {
            Tache pred = new Tache();
            pred.setNom("Prédécesseur");
            pred.setStatutKanban(StatutKanban.BACKLOG);
            tacheEntite.getPredecesseurs().add(pred);
            tacheEntite.setStatutKanban(StatutKanban.EN_COURS);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));

            assertThatThrownBy(() -> tacheService.changerStatut(1L, StatutKanban.TERMINE))
                    .isInstanceOf(TacheValidationException.class)
                    .hasMessageContaining("R5");
        }

        @Test
        @DisplayName("R5 — autorise le passage EN_COURS si tous les prédécesseurs sont TERMINE")
        void r5_autorise_si_tous_predecesseurs_termines() {
            Tache pred = new Tache();
            pred.setNom("Prédécesseur terminé");
            pred.setStatutKanban(StatutKanban.TERMINE);
            tacheEntite.getPredecesseurs().add(pred);
            tacheEntite.setStatutKanban(StatutKanban.BACKLOG);
            tacheEntite.setDateDebut(null);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            assertThatCode(() -> tacheService.changerStatut(1L, StatutKanban.EN_COURS))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Propagation EN_COURS → ascendants passent EN_COURS + R3 appliquée")
        void propagation_en_cours_vers_ascendants() {
            Tache parent = new Tache();
            parent.setNom("Parent");
            parent.setStatutKanban(StatutKanban.BACKLOG);
            parent.setDateDebut(null);
            tacheEntite.setParent(parent);
            tacheEntite.setStatutKanban(StatutKanban.BACKLOG);
            tacheEntite.setDateDebut(null);
            parent.getSousTaches().add(tacheEntite);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            tacheService.changerStatut(1L, StatutKanban.EN_COURS);

            assertThat(parent.getStatutKanban()).isEqualTo(StatutKanban.EN_COURS);
            assertThat(parent.getDateDebut()).isEqualTo(LocalDate.now()); // R3 ascendant
        }

        @Test
        @DisplayName("Propagation TERMINE → ascendant passe TERMINE + R4 si tous descendants terminés")
        void propagation_termine_vers_ascendant_si_tous_termines() {
            Tache parent = new Tache();
            parent.setNom("Parent");
            parent.setStatutKanban(StatutKanban.EN_COURS);
            parent.setDateFin(LocalDate.now().plusDays(5));

            tacheEntite.setParent(parent);
            tacheEntite.setStatutKanban(StatutKanban.EN_COURS);
            parent.getSousTaches().add(tacheEntite);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            tacheService.changerStatut(1L, StatutKanban.TERMINE);

            assertThat(parent.getStatutKanban()).isEqualTo(StatutKanban.TERMINE);
            assertThat(parent.getDateFin()).isEqualTo(LocalDate.now()); // R4 ascendant
        }

        @Test
        @DisplayName("Propagation TERMINE → ascendant reste EN_COURS si d'autres sous-tâches ne sont pas terminées")
        void propagation_termine_ne_touche_pas_ascendant_si_sous_taches_non_terminees() {
            Tache soeur = new Tache();
            soeur.setNom("Sœur non terminée");
            soeur.setStatutKanban(StatutKanban.EN_COURS);

            Tache parent = new Tache();
            parent.setNom("Parent");
            parent.setStatutKanban(StatutKanban.EN_COURS);

            tacheEntite.setParent(parent);
            tacheEntite.setStatutKanban(StatutKanban.EN_COURS);
            parent.getSousTaches().add(tacheEntite);
            parent.getSousTaches().add(soeur); // sœur pas encore terminée

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            tacheService.changerStatut(1L, StatutKanban.TERMINE);

            assertThat(parent.getStatutKanban()).isEqualTo(StatutKanban.EN_COURS);
        }

        @Test
        @DisplayName("changerStatut retourne la liste des tâches ascendantes auto-terminées")
        void doit_retourner_liste_auto_termines() {
            Tache parent = new Tache();
            parent.setNom("Projet Alpha");
            parent.setStatutKanban(StatutKanban.EN_COURS);
            parent.setDateFin(null);

            tacheEntite.setParent(parent);
            tacheEntite.setStatutKanban(StatutKanban.EN_COURS);
            parent.getSousTaches().add(tacheEntite);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            when(tacheRepository.save(any())).thenReturn(tacheEntite);

            List<String> autoTermines = tacheService.changerStatut(1L, StatutKanban.TERMINE);

            assertThat(autoTermines).contains("Projet Alpha");
        }
    }

    // =========================================================================
    // verifierImpactSuccesseurs() — R6
    // =========================================================================

    @Nested
    @DisplayName("verifierImpactSuccesseurs() — R6")
    class VerifierImpactSuccesseurs {

        @Test
        @DisplayName("R6 — retourne le nom du successeur dont dateDebut < nouvelle dateFin")
        void r6_detecte_successeur_impacte() {
            Tache successeur = new Tache();
            successeur.setNom("Tâche suivante");
            successeur.setDateDebut(LocalDate.now().plusDays(5));
            tacheEntite.getSuccesseurs().add(successeur);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));

            // Nouvelle dateFin = dans 10 jours → successeur (debut+5) serait impacté
            List<String> impactes = tacheService.verifierImpactSuccesseurs(1L,
                    LocalDate.now().plusDays(10));

            assertThat(impactes).contains("Tâche suivante");
        }

        @Test
        @DisplayName("R6 — retourne liste vide si aucun successeur n'est impacté")
        void r6_aucun_successeur_impacte() {
            Tache successeur = new Tache();
            successeur.setNom("Tâche suivante");
            successeur.setDateDebut(LocalDate.now().plusDays(20));
            tacheEntite.getSuccesseurs().add(successeur);

            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));

            // Nouvelle dateFin = dans 10 jours → successeur (debut+20) n'est pas impacté
            List<String> impactes = tacheService.verifierImpactSuccesseurs(1L,
                    LocalDate.now().plusDays(10));

            assertThat(impactes).isEmpty();
        }

        @Test
        @DisplayName("R6 — retourne liste vide si nouvelle dateFin est null")
        void r6_retourne_vide_si_dateFin_null() {
            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));

            List<String> impactes = tacheService.verifierImpactSuccesseurs(1L, null);

            assertThat(impactes).isEmpty();
        }
    }

    // =========================================================================
    // supprimer()
    // =========================================================================

    @Nested
    @DisplayName("supprimer()")
    class Supprimer {

        @Test
        @DisplayName("Supprime la tâche quand elle existe")
        void doit_supprimer_tache_existante() {
            when(tacheRepository.findById(1L)).thenReturn(Optional.of(tacheEntite));
            tacheService.supprimer(1L);
            verify(tacheRepository).delete(tacheEntite);
        }

        @Test
        @DisplayName("Lève TacheNotFoundException si tâche introuvable")
        void doit_lever_exception_si_tache_introuvable() {
            when(tacheRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> tacheService.supprimer(99L))
                    .isInstanceOf(TacheNotFoundException.class);
            verify(tacheRepository, never()).delete(any());
        }
    }

    // =========================================================================
    // rechercherParNom()
    // =========================================================================

    @Nested
    @DisplayName("rechercherParNom()")
    class RechercherParNom {

        @Test
        @DisplayName("Retourne les tâches dont le nom contient le terme")
        void doit_retourner_taches_correspondantes() {
            when(tacheRepository.findByParentIsNullAndNomContainingIgnoreCase("auth"))
                    .thenReturn(List.of(tacheEntite));
            when(tacheMapper.toDTOList(List.of(tacheEntite))).thenReturn(List.of(tacheDTO));

            List<TacheDTO> res = tacheService.rechercherParNom("auth");

            assertThat(res).hasSize(1);
        }

        @Test
        @DisplayName("Retourne liste vide si aucune correspondance")
        void doit_retourner_liste_vide_si_aucune_correspondance() {
            when(tacheRepository.findByParentIsNullAndNomContainingIgnoreCase("xyz"))
                    .thenReturn(List.of());
            when(tacheMapper.toDTOList(List.of())).thenReturn(List.of());

            assertThat(tacheService.rechercherParNom("xyz")).isEmpty();
        }
    }
}