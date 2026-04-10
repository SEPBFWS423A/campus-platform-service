package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AdminModuleResponse;
import de.campusplatform.campus_platform_service.dto.ModuleRequest;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.enums.Role;
import de.campusplatform.campus_platform_service.model.*;
import de.campusplatform.campus_platform_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final AppUserRepository userRepository;
    private final CourseOfStudyRepository courseOfStudyRepository;
    private final SpecializationRepository specializationRepository;
    private final ExamTypeRepository examTypeRepository;

    public List<AdminModuleResponse> getAllModulesForAdmin() {
        return moduleRepository.findAll().stream()
                .map((de.campusplatform.campus_platform_service.model.Module module) -> {
                    List<AdminModuleResponse.ExamTypeDTO> examTypes = module.getPossibleExamTypes().stream()
                            .map(type -> new AdminModuleResponse.ExamTypeDTO(
                                type.getId(), 
                                type.getType(), 
                                type.getNameDe(), 
                                type.getNameEn(), 
                                type.getShortDe(), 
                                type.getShortEn()))
                            .collect(Collectors.toList());

                    List<AdminModuleResponse.LecturerDTO> lecturers = module.getQualifiedLecturers().stream()
                            .map(ql -> new AdminModuleResponse.LecturerDTO(
                                    ql.getLecturer().getId(),
                                    ql.getLecturer().getTitle(),
                                    ql.getLecturer().getFirstName(),
                                    ql.getLecturer().getLastName()
                            ))
                            .collect(Collectors.toList());

                    return new AdminModuleResponse(
                            module.getId(),
                            module.getName(),
                            module.getSemester(),
                            module.getRequiredTotalHours(),
                            examTypes,
                            module.getPreferredExamType() != null ? module.getPreferredExamType().getId() : null,
                            lecturers,
                            module.getCourseOfStudy().getId(),
                            module.getSpecialization() != null ? module.getSpecialization().getId() : null
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void createModule(ModuleRequest request) {
        CourseOfStudy cos = courseOfStudyRepository.findById(request.courseOfStudyId())
                .orElseThrow(() -> new AppException("error.courseOfStudy.notFound"));

        Specialization spec = null;
        if (request.specializationId() != null) {
            spec = specializationRepository.findById(request.specializationId())
                    .orElseThrow(() -> new AppException("error.specialization.notFound"));
        }

        de.campusplatform.campus_platform_service.model.Module module = de.campusplatform.campus_platform_service.model.Module.builder()
                .name(request.name())
                .semester(request.semester())
                .requiredTotalHours(request.requiredTotalHours())
                .possibleExamTypes(request.examTypeIds() != null ? 
                    request.examTypeIds().stream()
                        .map(etId -> examTypeRepository.findById(etId).orElseThrow())
                        .collect(Collectors.toSet()) : Set.of())
                .courseOfStudy(cos)
                .specialization(spec)
                .preferredExamType(request.preferredExamTypeId() != null ? 
                    examTypeRepository.findById(request.preferredExamTypeId()).orElseThrow() : null)
                .build();

        de.campusplatform.campus_platform_service.model.Module savedModule = moduleRepository.save(module);

        if (request.lecturerIds() != null) {
            Set<ModuleLecturer> lecturers = request.lecturerIds().stream()
                    .map(id -> {
                        AppUser lecturer = userRepository.findById(id)
                                .orElseThrow(() -> new AppException("error.user.notFound"));
                        if (lecturer.getRole() != Role.LECTURER) {
                            throw new AppException("error.user.notLecturer");
                        }
                        return ModuleLecturer.builder()
                                .module(savedModule)
                                .lecturer(lecturer)
                                .grantedAt(LocalDateTime.now())
                                .build();
                    })
                    .collect(Collectors.toSet());
            savedModule.setQualifiedLecturers(lecturers);
            moduleRepository.save(savedModule);
        }
    }

    @Transactional
    public AdminModuleResponse updateModule(Long id, ModuleRequest request) {
        de.campusplatform.campus_platform_service.model.Module module = moduleRepository.findById(id)
                .orElseThrow(() -> new AppException("error.module.notFound"));

        CourseOfStudy cos = courseOfStudyRepository.findById(request.courseOfStudyId())
                .orElseThrow(() -> new AppException("error.courseOfStudy.notFound"));

        Specialization spec = null;
        if (request.specializationId() != null) {
            spec = specializationRepository.findById(request.specializationId())
                    .orElseThrow(() -> new AppException("error.specialization.notFound"));
        }

        module.setName(request.name());
        module.setSemester(request.semester());
        module.setRequiredTotalHours(request.requiredTotalHours());
        module.setPossibleExamTypes(request.examTypeIds() != null ? 
                request.examTypeIds().stream()
                    .map(etId -> examTypeRepository.findById(etId).orElseThrow())
                    .collect(Collectors.toSet()) : Set.of());
        module.setCourseOfStudy(cos);
        module.setSpecialization(spec);
        module.setPreferredExamType(request.preferredExamTypeId() != null ? 
                examTypeRepository.findById(request.preferredExamTypeId()).orElseThrow() : null);

        de.campusplatform.campus_platform_service.model.Module savedModule = moduleRepository.save(module);

        if (request.lecturerIds() != null) {
            // Update qualified lecturers
            savedModule.getQualifiedLecturers().clear();
            Set<ModuleLecturer> lecturers = request.lecturerIds().stream()
                    .map(lid -> {
                        AppUser lecturer = userRepository.findById(lid)
                                .orElseThrow(() -> new AppException("error.user.notFound"));
                        return ModuleLecturer.builder()
                                .module(savedModule)
                                .lecturer(lecturer)
                                .grantedAt(LocalDateTime.now())
                                .build();
                    })
                    .collect(Collectors.toSet());
            savedModule.getQualifiedLecturers().addAll(lecturers);
            moduleRepository.save(savedModule);
        }

        return getAllModulesForAdmin().stream()
                .filter(m -> m.id().equals(savedModule.getId()))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public void deleteModule(Long id) {
        if (!moduleRepository.existsById(id)) {
            throw new AppException("error.module.notFound");
        }
        try {
            moduleRepository.deleteById(id);
            moduleRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new AppException("error.module.referenced");
        }
    }
}
