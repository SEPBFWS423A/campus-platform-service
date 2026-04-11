package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AdminCourseSeriesResponse;
import de.campusplatform.campus_platform_service.dto.CourseSeriesRequest;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.enums.ExamStatus;
import de.campusplatform.campus_platform_service.enums.CourseStatus;
import de.campusplatform.campus_platform_service.enums.Role;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.CourseSeries;
import de.campusplatform.campus_platform_service.model.ExamType;
import de.campusplatform.campus_platform_service.model.Module;
import de.campusplatform.campus_platform_service.model.StudyGroup;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.CourseSeriesRepository;
import de.campusplatform.campus_platform_service.repository.ExamTypeRepository;
import de.campusplatform.campus_platform_service.repository.ModuleRepository;
import de.campusplatform.campus_platform_service.repository.StudyGroupRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import de.campusplatform.campus_platform_service.enums.EventType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseSeriesService {

    private final CourseSeriesRepository courseSeriesRepository;
    private final ModuleRepository moduleRepository;
    private final AppUserRepository appUserRepository;
    private final ExamTypeRepository examTypeRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final StudentSubmissionService studentSubmissionService;
    private final EventRepository eventRepository;

    public CourseSeriesService(CourseSeriesRepository courseSeriesRepository, 
                               ModuleRepository moduleRepository, 
                               AppUserRepository appUserRepository, 
                               ExamTypeRepository examTypeRepository, 
                               StudyGroupRepository studyGroupRepository,
                               StudentSubmissionService studentSubmissionService,
                               EventRepository eventRepository) {
        this.courseSeriesRepository = courseSeriesRepository;
        this.moduleRepository = moduleRepository;
        this.appUserRepository = appUserRepository;
        this.examTypeRepository = examTypeRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.studentSubmissionService = studentSubmissionService;
        this.eventRepository = eventRepository;
    }

    public List<AdminCourseSeriesResponse> getAllCourseSeries() {
        return courseSeriesRepository.findAll().stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    public AdminCourseSeriesResponse getCourseSeriesById(Long id) {
        CourseSeries courseSeries = courseSeriesRepository.findById(id)
                .orElseThrow(() -> new AppException("Course Series not found"));
        return mapToAdminResponse(courseSeries);
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
        
        CourseStatus oldStatus = courseSeries.getStatus();
        mapRequestToEntity(request, courseSeries);
        CourseSeries saved = courseSeriesRepository.save(courseSeries);
        
        // Handle student submission initialization when moving to ACTIVE
        if (oldStatus == CourseStatus.PLANNED && saved.getStatus() == CourseStatus.ACTIVE) {
            ExamType examType = saved.getSelectedExamType();
            if (examType == null) {
                // Fallback to module's preferred exam type if not set on series
                examType = saved.getModule().getPreferredExamType();
            }

            if (examType != null) {
                if (examType.isSubmission()) {
                    // SUBMISSION types initialize immediately upon ACTIVE
                    studentSubmissionService.initializeSubmissionsForCourseSeries(saved.getId());
                }
            }
        }
        
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
        
        if (request.status() == CourseStatus.COMPLETED && entity.getExamStatus() != ExamStatus.COMPLETED) {
            entity.setStatus(CourseStatus.GRADING);
        } else {
            entity.setStatus(request.status());
        }

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
