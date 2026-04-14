package de.campusplatform.campus_platform_service.enums;

public enum AbsenceStatus {
    BEANTRAGT,      // Eingereicht, wartet auf Genehmigung
    GENEHMIGT,      // Vom Admin genehmigt
    ABGELEHNT,      // Vom Admin abgelehnt
    STORNIERT,      // Vom Dozenten zurückgezogen
    ABGESCHLOSSEN   // Abwesenheit liegt in der Vergangenheit
}
