-- =============================================================================
-- DATA-TEST.SQL — Jeu de données d'exemple (profil test)
-- Chargé uniquement en mode test via spring.sql.init.data-locations
-- Utilise MERGE INTO pour éviter les doublons lors des redémarrages
-- =============================================================================

-- Tâche racine 1 : Projet Site Web
MERGE INTO tache (id, nom, description, statut_kanban, date_creation, date_debut, date_fin, duree, parent_id)
KEY (id) VALUES
(1, 'Projet Site Web', 'Refonte complète du site vitrine', 'EN_COURS',
 TIMESTAMPADD(DAY, -30, CURRENT_TIMESTAMP),
 DATEADD(DAY, -25, CURRENT_DATE),
 DATEADD(DAY, 30, CURRENT_DATE),
 60, NULL);

-- Sous-tâches du Projet Site Web
MERGE INTO tache (id, nom, description, statut_kanban, date_creation, date_debut, date_fin, duree, parent_id)
KEY (id) VALUES
(2, 'Maquettes', 'Création des maquettes Figma', 'TERMINE',
 TIMESTAMPADD(DAY, -28, CURRENT_TIMESTAMP),
 DATEADD(DAY, -25, CURRENT_DATE),
 DATEADD(DAY, -15, CURRENT_DATE),
 10, 1),
(3, 'Développement front', 'Intégration HTML/CSS/JS', 'EN_COURS',
 TIMESTAMPADD(DAY, -20, CURRENT_TIMESTAMP),
 DATEADD(DAY, -14, CURRENT_DATE),
 DATEADD(DAY, 5, CURRENT_DATE),
 20, 1),
(4, 'Recette', 'Tests et corrections', 'BACKLOG',
 TIMESTAMPADD(DAY, -20, CURRENT_TIMESTAMP),
 DATEADD(DAY, 6, CURRENT_DATE),
 DATEADD(DAY, 15, CURRENT_DATE),
 10, 1);

-- Tâche racine 2 : Formation Java Spring
MERGE INTO tache (id, nom, description, statut_kanban, date_creation, date_debut, date_fin, duree, parent_id)
KEY (id) VALUES
(5, 'Formation Java Spring', 'Coaching Java Spring Boot', 'BACKLOG',
 TIMESTAMPADD(DAY, -10, CURRENT_TIMESTAMP),
 DATEADD(DAY, -2, CURRENT_DATE),
 DATEADD(DAY, 10, CURRENT_DATE),
 5, NULL);

-- Sous-tâches de Formation Java Spring
MERGE INTO tache (id, nom, description, statut_kanban, date_creation, date_debut, date_fin, duree, parent_id)
KEY (id) VALUES
(6, 'Préparation support', 'Préparer les slides et exercices', 'BACKLOG',
 TIMESTAMPADD(DAY, -8, CURRENT_TIMESTAMP),
 DATEADD(DAY, -2, CURRENT_DATE),
 DATEADD(DAY, 0, CURRENT_DATE),
 2, 5),
(7, 'Animation J1', 'Première journée de formation', 'BACKLOG',
 TIMESTAMPADD(DAY, -8, CURRENT_TIMESTAMP),
 DATEADD(DAY, 1, CURRENT_DATE),
 DATEADD(DAY, 1, CURRENT_DATE),
 1, 5),
(8, 'Animation J2', 'Deuxième journée de formation', 'BACKLOG',
 TIMESTAMPADD(DAY, -8, CURRENT_TIMESTAMP),
 DATEADD(DAY, 2, CURRENT_DATE),
 DATEADD(DAY, 2, CURRENT_DATE),
 1, 5);

-- Tâche racine 3 : Tâche orpheline en retard (pour tester les urgences)
MERGE INTO tache (id, nom, description, statut_kanban, date_creation, date_debut, date_fin, duree, parent_id)
KEY (id) VALUES
(9, 'Mise à jour serveur', 'Mise à jour des dépendances serveur', 'BACKLOG',
 TIMESTAMPADD(DAY, -15, CURRENT_TIMESTAMP),
 DATEADD(DAY, -5, CURRENT_DATE),
 DATEADD(DAY, -1, CURRENT_DATE),
 1, NULL);

-- Dépendance : Recette dépend de Développement front
MERGE INTO tache_predecesseur (tache_id, predecesseur_id)
KEY (tache_id, predecesseur_id) VALUES (4, 3);

-- Dépendance : Animation J1 dépend de Préparation support
MERGE INTO tache_predecesseur (tache_id, predecesseur_id)
KEY (tache_id, predecesseur_id) VALUES (7, 6);

-- Dépendance : Animation J2 dépend de Animation J1
MERGE INTO tache_predecesseur (tache_id, predecesseur_id)
KEY (tache_id, predecesseur_id) VALUES (8, 7);
