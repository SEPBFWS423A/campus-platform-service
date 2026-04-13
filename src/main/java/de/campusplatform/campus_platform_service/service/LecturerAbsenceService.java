package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.LecturerAbsenceRequest;
import de.campusplatform.campus_platform_service.dto.LecturerAbsenceResponse;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.LecturerAbsence;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
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

    public LecturerAbsenceService(LecturerAbsenceRepository lecturerAbsenceRepository,
                                   AppUserRepository userRepository) {
        this.lecturerAbsenceRepository = lecturerAbsenceRepository;
        this.userRepository = userRepository;
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

    @Transactional
    public LecturerAbsenceResponse createAbsence(Long lecturerId, LecturerAbsenceRequest request) {
        AppUser lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new AppException("Lecturer not found"));

        LocalDateTime startDateTime = request.startDate().atStartOfDay();
        LocalDateTime endDateTime = request.endDate().atTime(23, 59, 59);

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
