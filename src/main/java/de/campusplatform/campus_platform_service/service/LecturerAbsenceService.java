package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.ConflictingEventDto;
import de.campusplatform.campus_platform_service.dto.LecturerAbsenceRequest;
import de.campusplatform.campus_platform_service.dto.LecturerAbsenceResponse;
import de.campusplatform.campus_platform_service.enums.AbsenceAuditAction;
import de.campusplatform.campus_platform_service.enums.AbsencePriority;
import de.campusplatform.campus_platform_service.enums.AbsenceStatus;
import de.campusplatform.campus_platform_service.enums.AbsenceType;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.exception.LecturerAbsenceConflictException;
import de.campusplatform.campus_platform_service.model.AbsenceAuditLog;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.Event;
import de.campusplatform.campus_platform_service.model.LecturerAbsence;
import de.campusplatform.campus_platform_service.repository.AbsenceAuditLogRepository;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import de.campusplatform.campus_platform_service.repository.LecturerAbsenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LecturerAbsenceService {

    private final LecturerAbsenceRepository lecturerAbsenceRepository;
    private final AppUserRepository userRepository;
    private final EventRepository eventRepository;
    private final AbsenceAuditLogRepository auditLogRepository;

    public LecturerAbsenceService(LecturerAbsenceRepository lecturerAbsenceRepository,
                                   AppUserRepository userRepository,
                                   EventRepository eventRepository,
                                   AbsenceAuditLogRepository auditLogRepository) {
        this.lecturerAbsenceRepository = lecturerAbsenceRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<LecturerAbsenceResponse> getMyAbsences(Long lecturerId) {
        return lecturerAbsenceRepository.findByLecturerId(lecturerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LecturerAbsenceResponse> getAllAbsences() {
        return lecturerAbsenceRepository.findAllByOrderByStartDateAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gibt alle Events des Dozenten zurueck, die im Zeitraum stattfinden.
     * Vorab-Check (GET /absences/conflicts) + createAbsence().
     */
    @Transactional(readOnly = true)
    public List<ConflictingEventDto> getConflictingEvents(Long lecturerId, LocalDateTime start, LocalDateTime end) {
        List<Event> events = eventRepository.findOverlappingEventsForLecturer(lecturerId, start, end, null);
        return events.stream()
                .map(e -> {
                    String eventName;
                    if (e.getName() != null && !e.getName().isBlank()) {
                        eventName = e.getName();
                    } else if (e.getCourseSeries() != null && e.getCourseSeries().getModule() != null) {
                        eventName = e.getCourseSeries().getModule().getName();
                    } else {
                        eventName = "Veranstaltung #" + e.getId();
                    }
                    LocalDateTime eventEnd = e.getStartTime().plusMinutes(
                            e.getDurationMinutes() != null ? e.getDurationMinutes() : 0);
                    return new ConflictingEventDto(e.getId(), eventName, e.getStartTime(), eventEnd);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public LecturerAbsenceResponse createAbsence(Long lecturerId, LecturerAbsenceRequest request, boolean force) {
        AppUser lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new AppException("Lecturer not found"));

        LocalDateTime startDateTime = request.startDate().atStartOfDay();
        LocalDateTime endDateTime   = request.endDate().atTime(23, 59, 59);

        // --- Sub-Issue #291: Validierungsregeln je Abwesenheitsart ---
        validateAbsenceRules(request.type(), request.startDate(), request.endDate());

        if (!force) {
            List<ConflictingEventDto> conflicts = getConflictingEvents(lecturerId, startDateTime, endDateTime);
            if (!conflicts.isEmpty()) {
                throw new LecturerAbsenceConflictException(
                        "Terminkonflikt: In diesem Zeitraum sind bereits Lehrveranstaltungen geplant.",
                        conflicts);
            }
        }

        // Vorlaufzeit berechnen
        long noticeDays = ChronoUnit.DAYS.between(LocalDate.now(), request.startDate());

        // Dokumentationspflicht ermitteln
        boolean docRequired = computeDocumentRequired(request.type(), request.startDate(), request.endDate());

        // Priorität aus Request übernehmen oder Standardwert
        AbsencePriority priority = request.priority() != null ? request.priority() : AbsencePriority.MEDIUM;

        LecturerAbsence absence = LecturerAbsence.builder()
                .lecturer(lecturer)
                .type(request.type())
                .startDate(startDateTime)
                .endDate(endDateTime)
                .note(request.note())
                .status(AbsenceStatus.BEANTRAGT)
                .priority(priority)
                .noticeDays((int) Math.max(0, noticeDays))
                .documentRequired(docRequired)
                .build();

        LecturerAbsence saved = lecturerAbsenceRepository.save(absence);

        // Audit-Log: CREATED
        writeAuditLog(saved.getId(), AbsenceAuditAction.CREATED, null, AbsenceStatus.BEANTRAGT,
                lecturer.getEmail(), null);

        return mapToResponse(saved);
    }

    /** Rückwärtskompatible Überladung ohne force-Parameter. */
    @Transactional
    public LecturerAbsenceResponse createAbsence(Long lecturerId, LecturerAbsenceRequest request) {
        return createAbsence(lecturerId, request, false);
    }

    // --- Sub-Issue #292: Genehmigungsworkflow ---

    @Transactional
    public LecturerAbsenceResponse approveAbsence(Long absenceId, String approvedBy) {
        LecturerAbsence absence = lecturerAbsenceRepository.findById(absenceId)
                .orElseThrow(() -> new AppException("Absence not found"));

        if (absence.getStatus() != AbsenceStatus.BEANTRAGT) {
            throw new LecturerAbsenceConflictException(
                    "Nur Abwesenheiten mit Status BEANTRAGT können genehmigt werden.",
                    List.of());
        }

        AbsenceStatus prev = absence.getStatus();
        absence.setStatus(AbsenceStatus.GENEHMIGT);
        absence.setApprovedBy(approvedBy);
        absence.setApprovedAt(LocalDateTime.now());
        LecturerAbsence saved = lecturerAbsenceRepository.save(absence);

        writeAuditLog(absenceId, AbsenceAuditAction.APPROVED, prev, AbsenceStatus.GENEHMIGT, approvedBy, null);

        return mapToResponse(saved);
    }

    @Transactional
    public LecturerAbsenceResponse rejectAbsence(Long absenceId, String reason, String rejectedBy) {
        LecturerAbsence absence = lecturerAbsenceRepository.findById(absenceId)
                .orElseThrow(() -> new AppException("Absence not found"));

        if (absence.getStatus() != AbsenceStatus.BEANTRAGT) {
            throw new LecturerAbsenceConflictException(
                    "Nur Abwesenheiten mit Status BEANTRAGT können abgelehnt werden.",
                    List.of());
        }

        AbsenceStatus prev = absence.getStatus();
        absence.setStatus(AbsenceStatus.ABGELEHNT);
        absence.setRejectionReason(reason);
        absence.setApprovedBy(rejectedBy);
        absence.setApprovedAt(LocalDateTime.now());
        LecturerAbsence saved = lecturerAbsenceRepository.save(absence);

        writeAuditLog(absenceId, AbsenceAuditAction.REJECTED, prev, AbsenceStatus.ABGELEHNT, rejectedBy, reason);

        return mapToResponse(saved);
    }

    // --- Sub-Issue #297: Audit-Trail abrufen ---

    @Transactional(readOnly = true)
    public List<AbsenceAuditLog> getHistory(Long absenceId) {
        return auditLogRepository.findByAbsenceIdOrderByPerformedAtAsc(absenceId);
    }

    @Transactional
    public void deleteAbsence(Long absenceId, Long lecturerId, boolean isAdmin) {
        LecturerAbsence absence = lecturerAbsenceRepository.findById(absenceId)
                .orElseThrow(() -> new AppException("Absence not found"));

        if (!isAdmin && !absence.getLecturer().getId().equals(lecturerId)) {
            throw new AppException("Not authorized to delete this absence");
        }

        lecturerAbsenceRepository.delete(absence);
    }

    public boolean hasAbsenceConflict(Long lecturerId, LocalDateTime start, LocalDateTime end) {
        List<LecturerAbsence> conflicts = lecturerAbsenceRepository
                .findByLecturerIdAndEndDateAfterAndStartDateBefore(lecturerId, start, end);
        return !conflicts.isEmpty();
    }

    // -----------------------------------------------------------------------
    // Hilfsmethoden
    // -----------------------------------------------------------------------

    /**
     * Sub-Issue #291 – Validierungsregeln je AbsenceType.
     * Wirft AppException bei Verletzung.
     */
    private void validateAbsenceRules(AbsenceType type, LocalDate start, LocalDate end) {
        long noticeDays = ChronoUnit.DAYS.between(LocalDate.now(), start);
        long durationDays = ChronoUnit.DAYS.between(start, end) + 1;

        switch (type) {
            case URLAUB -> {
                if (noticeDays < 14) {
                    throw new AppException("absence.validation.urlaub.notice");
                }
                if (durationDays > 30) {
                    throw new AppException("absence.validation.urlaub.maxDuration");
                }
            }
            case DIENSTREISE -> {
                if (noticeDays < 7) {
                    throw new AppException("absence.validation.dienstreise.notice");
                }
            }
            case KRANKMELDUNG, SONSTIGES -> {
                // Kein Mindest-Vorlauf erforderlich
            }
        }
    }

    /**
     * Sub-Issue #296 – Dokumentationspflicht je Abwesenheitsart ermitteln.
     */
    private boolean computeDocumentRequired(AbsenceType type, LocalDate start, LocalDate end) {
        long durationDays = ChronoUnit.DAYS.between(start, end) + 1;
        return switch (type) {
            case KRANKMELDUNG -> durationDays > 3;
            case DIENSTREISE  -> true;
            default           -> false;
        };
    }

    private void writeAuditLog(Long absenceId, AbsenceAuditAction action,
                                AbsenceStatus prev, AbsenceStatus next,
                                String performedBy, String reason) {
        auditLogRepository.save(AbsenceAuditLog.builder()
                .absenceId(absenceId)
                .action(action)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .previousStatus(prev)
                .newStatus(next)
                .reason(reason)
                .build());
    }

    private LecturerAbsenceResponse mapToResponse(LecturerAbsence absence) {
        String lecturerName = absence.getLecturer() != null ?
                absence.getLecturer().getFirstName() + " " + absence.getLecturer().getLastName() : "Unknown";

        return new LecturerAbsenceResponse(
                absence.getId(),
                absence.getType(),
                absence.getStartDate() != null ? absence.getStartDate().toLocalDate() : null,
                absence.getEndDate()   != null ? absence.getEndDate().toLocalDate()   : null,
                absence.getNote(),
                lecturerName,
                absence.getStatus(),
                absence.getPriority(),
                absence.getDocumentRequired(),
                absence.getApprovedBy(),
                absence.getRejectionReason()
        );
    }
}
