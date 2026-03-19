package fr.oakrise.oaktime.entity;

/**
 * Statut Kanban d'une tâche.
 * Représente la position de la tâche dans le tableau Kanban.
 * Toute tâche est créée avec le statut BACKLOG par défaut.
 */
public enum StatutKanban {
    BACKLOG,
    EN_COURS,
    TERMINE
}