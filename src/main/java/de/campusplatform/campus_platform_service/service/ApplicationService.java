package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.ApplicationResponse;
import de.campusplatform.campus_platform_service.dto.StudyProgramResponse;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.Application;
import de.campusplatform.campus_platform_service.model.ApplicationStatus;
import de.campusplatform.campus_platform_service.model.StudyProgram;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.ApplicationRepository;
import de.campusplatform.campus_platform_service.repository.StudyProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import de.campusplatform.campus_platform_service.dto.AdminApplicationResponse;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final StudyProgramRepository studyProgramRepository;
    private final AppUserRepository userRepository;

    private final Path uploadDir = Paths.get("uploads/applications");

    public ApplicationService(
        ApplicationRepository applicationRepository,
        StudyProgramRepository studyProgramRepository,
        AppUserRepository userRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.studyProgramRepository = studyProgramRepository;
        this.userRepository = userRepository;
    }

    public List<StudyProgramResponse> getActivePrograms() {
        return studyProgramRepository.findByActiveTrue()
            .stream()
            .map(StudyProgramResponse::from)
            .toList();
    }

    public List<ApplicationResponse> getMyApplications(Long studentId) {
        return applicationRepository.findByStudentId(studentId)
            .stream()
            .map(ApplicationResponse::from)
            .toList();
    }

    public ApplicationResponse apply(
        Long studentId,
        Long programId,
        String motivation,
        Integer priority,
        MultipartFile file
    ) {
        if (applicationRepository.existsByStudentIdAndProgramId(studentId, programId)) {
            throw new IllegalStateException("Du hast dich bereits für diesen Studiengang beworben.");
        }

        AppUser student = userRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden"));

        StudyProgram program = studyProgramRepository.findById(programId)
            .orElseThrow(() -> new IllegalArgumentException("Studiengang nicht gefunden"));

        Application application = new Application();
        application.setStudent(student);
        application.setProgram(program);
        application.setMotivation(motivation);
        application.setPriority(priority != null ? priority : 1);

        if (file != null && !file.isEmpty()) {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            try {
                Files.createDirectories(uploadDir);
                Files.copy(file.getInputStream(), uploadDir.resolve(filename));
                application.setDocumentPath(filename);
            } catch (IOException e) {
                throw new RuntimeException("Datei konnte nicht gespeichert werden", e);
            }
        }

        return ApplicationResponse.from(applicationRepository.save(application));
    }
    public List<AdminApplicationResponse> getAllApplications() {
    return applicationRepository.findAll()
        .stream()
        .map(AdminApplicationResponse::from)
        .toList();
}

public AdminApplicationResponse updateStatus(Long id, String status) {
    Application application = applicationRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Bewerbung nicht gefunden"));
    application.setStatus(ApplicationStatus.valueOf(status));
    return AdminApplicationResponse.from(applicationRepository.save(application));
}
}