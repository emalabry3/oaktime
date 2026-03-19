package fr.oakrise.oaktime.entity;

/**
 * Type d'une trace dans le journal d'une tâche.
 */
public enum TypeTrace {
    CREATION,
    MODIFICATION,
    STATUT,
    PREDECESSEUR,
    SOUS_TACHE,
    MANUEL
}