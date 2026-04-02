package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AdminCourseSeriesResponse;
import de.campusplatform.campus_platform_service.dto.CourseSeriesRequest;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.CourseSeries;
import de.campusplatform.campus_platform_service.model.ExamType;
import de.campusplatform.campus_platform_service.model.Module;
import de.campusplatform.campus_platform_service.model.Role;
import de.campusplatform.campus_platform_service.model.StudyGroup;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.CourseSeriesRepository;
import de.campusplatform.campus_platform_service.repository.ExamTypeRepository;
import de.campusplatform.campus_platform_service.repository.ModuleRepository;
import de.campusplatform.campus_platform_service.repository.StudyGroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseSeriesService {

    private final CourseSeriesRepository courseSeriesRepository;
    private final ModuleRepository moduleRepository;
    private final AppUserRepository appUserRepository;
    private final ExamTypeRepository examTypeRepository;
    private final StudyGroupRepository studyGroupRepository;

    public CourseSeriesService(CourseSeriesRepository courseSeriesRepository, ModuleRepository moduleRepository, AppUserRepository appUserRepository, ExamTypeRepository examTypeRepository, StudyGroupRepository studyGroupRepository) {
        this.courseSeriesRepository = courseSeriesRepository;
        this.moduleRepository = moduleRepository;
        this.appUserRepository = appUserRepository;
        this.examTypeRepository = examTypeRepository;
        this.studyGroupRepository = studyGroupRepository;
    }

    public List<AdminCourseSeriesResponse> getAllCourseSeries() {
        return courseSeriesRepository.findAll().stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    public AdminCourseSeriesResponse createCourseSeries(CourseSeriesRequest request) {
        CourseSeries courseSeries = new CourseSeries();
        mapRequestToEntity(request, courseSeries);
        CourseSeries saved = courseSeriesRepository.save(courseSeries);
        return mapToAdminResponse(saved);
    }

    public AdminCourseSeriesResponse updateCourseSeries(Long id, CourseSeriesRequest request) {
        CourseSeries courseSeries = courseSeriesRepository.findById(id)
                .orElseThrow(() -> new AppException("Course Series not found"));
        
        mapRequestToEntity(request, courseSeries);
        CourseSeries saved = courseSeriesRepository.save(courseSeries);
        return mapToAdminResponse(saved);
    }

    public void deleteCourseSeries(Long id) {
        if (!courseSeriesRepository.existsById(id)) {
            throw new AppException("Course Series not found");
        }
        courseSeriesRepository.deleteById(id);
    }

    private void mapRequestToEntity(CourseSeriesRequest request, CourseSeries entity) {
        Module module = moduleRepository.findById(request.moduleId())
                .orElseThrow(() -> new AppException("Module not found"));
        
        AppUser lecturer = appUserRepository.findById(request.assignedLecturerId())
                .orElseThrow(() -> new AppException("Lecturer not found"));
                
        if (lecturer.getRole() != Role.LECTURER) {
            throw new AppException("Assigned user must be a LECTURER");
        }

        boolean teachesModule = module.getQualifiedLecturers().stream()
                .anyMatch(ml -> ml.getLecturer().getId().equals(lecturer.getId()));
        if (!teachesModule) {
            throw new AppException("Lecturer does not teach the selected module");
        }

        ExamType examType = null;
        if (request.selectedExamTypeId() != null) {
            examType = examTypeRepository.findById(request.selectedExamTypeId())
                    .orElseThrow(() -> new AppException("Exam type not found"));
            
            final Long examTypeId = examType.getId();
            boolean isAllowed = module.getPossibleExamTypes().stream()
                    .anyMatch(et -> et.getId().equals(examTypeId));
            if (!isAllowed) {
                throw new AppException("Exam type is not allowed for this module");
            }
        }

        entity.setModule(module);
        entity.setAssignedLecturer(lecturer);
        entity.setStatus(request.status());
        entity.setSelectedExamType(examType);
        entity.setSubmissionStartDate(request.submissionStartDate());
        entity.setSubmissionDeadline(request.submissionDeadline());

        if (request.studyGroupIds() != null && !request.studyGroupIds().isEmpty()) {
            List<StudyGroup> groups = studyGroupRepository.findAllById(request.studyGroupIds());
            if (entity.getStudyGroups() == null) {
                entity.setStudyGroups(new java.util.HashSet<>());
            } else {
                entity.getStudyGroups().clear();
            }
            entity.getStudyGroups().addAll(groups);
        } else {
            if (entity.getStudyGroups() != null) {
                entity.getStudyGroups().clear();
            }
        }
    }

    private AdminCourseSeriesResponse mapToAdminResponse(CourseSeries entity) {
        List<AdminCourseSeriesResponse.StudyGroupDTO> studyGroupDTOs = entity.getStudyGroups() != null 
                ? entity.getStudyGroups().stream()
                    .map(sg -> new AdminCourseSeriesResponse.StudyGroupDTO(sg.getId(), sg.getName()))
                    .collect(Collectors.toList())
                : List.of();

        return new AdminCourseSeriesResponse(
                entity.getId(),
                entity.getModule() != null ? entity.getModule().getId() : null,
                entity.getModule() != null ? entity.getModule().getName() : null,
                entity.getAssignedLecturer() != null ? entity.getAssignedLecturer().getId() : null,
                entity.getAssignedLecturer() != null ? (entity.getAssignedLecturer().getFirstName() + " " + entity.getAssignedLecturer().getLastName()) : null,
                entity.getStatus(),
                entity.getSelectedExamType() != null ? entity.getSelectedExamType().getId() : null,
                entity.getSelectedExamType() != null ? entity.getSelectedExamType().getNameDe() : null, 
                entity.getSubmissionStartDate(),
                entity.getSubmissionDeadline(),
                studyGroupDTOs
        );
    }
}
