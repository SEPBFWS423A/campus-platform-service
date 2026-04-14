package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.ConflictingEventDto;
import de.campusplatform.campus_platform_service.dto.LecturerAbsenceRequest;
import de.campusplatform.campus_platform_service.dto.LecturerAbsenceResponse;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.exception.LecturerAbsenceConflictException;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.Event;
import de.campusplatform.campus_platform_service.model.LecturerAbsence;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import de.campusplatform.campus_platform_service.repository.LecturerAbsenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LecturerAbsenceService {

    private final LecturerAbsenceRepository lecturerAbsenceRepository;
    private final AppUserRepository userRepository;
    private final EventRepository eventRepository;

    public LecturerAbsenceService(LecturerAbsenceRepository lecturerAbsenceRepository,
                                   AppUserRepository userRepository,
                                   EventRepository eventRepository) {
        this.lecturerAbsenceRepository = lecturerAbsenceRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
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
     * Gibt alle Events des Dozenten zurueck, die im angegebenen Zeitraum stattfinden.
     * Wird sowohl fuer den Vorab-Check (GET /absences/conflicts) als auch im createAbsence() verwendet.
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
        LocalDateTime endDateTime = request.endDate().atTime(23, 59, 59);

        if (!force) {
            List<ConflictingEventDto> conflicts = getConflictingEvents(lecturerId, startDateTime, endDateTime);
            if (!conflicts.isEmpty()) {
                throw new LecturerAbsenceConflictException(
                        "Terminkonflikt: In diesem Zeitraum sind bereits Lehrveranstaltungen geplant.",
                        conflicts);
            }
        }

        LecturerAbsence absence = LecturerAbsence.builder()
                .lecturer(lecturer)
                .type(request.type())
                .startDate(startDateTime)
                .endDate(endDateTime)
                .note(request.note())
                .build();

        LecturerAbsence saved = lecturerAbsenceRepository.save(absence);
        return mapToResponse(saved);
    }

    /** Rueckwaertskompatible Ueberladung ohne force-Parameter (force = false). */
    @Transactional
    public LecturerAbsenceResponse createAbsence(Long lecturerId, LecturerAbsenceRequest request) {
        return createAbsence(lecturerId, request, false);
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

    private LecturerAbsenceResponse mapToResponse(LecturerAbsence absence) {
        String lecturerName = absence.getLecturer() != null ?
                absence.getLecturer().getFirstName() + " " + absence.getLecturer().getLastName() : "Unknown";

        return new LecturerAbsenceResponse(
                absence.getId(),
                absence.getType(),
                absence.getStartDate() != null ? absence.getStartDate().toLocalDate() : null,
                absence.getEndDate() != null ? absence.getEndDate().toLocalDate() : null,
                absence.getNote(),
                lecturerName
        );
    }
}
