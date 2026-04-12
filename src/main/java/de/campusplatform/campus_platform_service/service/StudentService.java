package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.StudentActiveCourseResponse;
import de.campusplatform.campus_platform_service.dto.StudentEventResponse;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.CourseSeries;
import de.campusplatform.campus_platform_service.model.Event;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.CourseSeriesRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final EventRepository eventRepository;
    private final CourseSeriesRepository courseSeriesRepository;
    private final AppUserRepository appUserRepository;

    public StudentService(EventRepository eventRepository,
                          CourseSeriesRepository courseSeriesRepository,
                          AppUserRepository appUserRepository) {
        this.eventRepository = eventRepository;
        this.courseSeriesRepository = courseSeriesRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<StudentEventResponse> getUpcomingEvents(String username) {
        AppUser user = appUserRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return eventRepository.findUpcomingEventsByStudentUserId(user.getId(), LocalDateTime.now())
                .stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    public List<StudentActiveCourseResponse> getActiveCourseSeries(String username) {
        AppUser user = appUserRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return courseSeriesRepository.findNonPlannedCourseSeriesByStudentUserId(user.getId())
                .stream()
                .map(this::mapToCourseResponse)
                .collect(Collectors.toList());
    }

    private StudentEventResponse mapToEventResponse(Event event) {
        String lecturerName = "TBD";
        if (event.getCourseSeries() != null && event.getCourseSeries().getAssignedLecturer() != null) {
            lecturerName = event.getCourseSeries().getAssignedLecturer().getFirstName() + " " + event.getCourseSeries().getAssignedLecturer().getLastName();
        }

        return new StudentEventResponse(
                event.getId(),
                event.getName(),
                event.getEventType(),
                event.getStartTime(),
                event.getDurationMinutes(),
                event.getCourseSeries() != null ? event.getCourseSeries().getModule().getName() : "Unknown",
                lecturerName,
                event.getRooms() != null ? event.getRooms().stream().map(Room::getName).collect(Collectors.toList()) : List.of()
        );
    }

    private StudentActiveCourseResponse mapToCourseResponse(CourseSeries series) {
        String lecturerName = "TBD";
        if (series.getAssignedLecturer() != null) {
            lecturerName = series.getAssignedLecturer().getFirstName() + " " + series.getAssignedLecturer().getLastName();
        }

        String examTypeName = "None";
        boolean isSubmission = false;
        if (series.getSelectedExamType() != null) {
            examTypeName = series.getSelectedExamType().getNameDe(); // Using German as default or based on context? 
            // Better to pass both or let frontend handle translation if we had the code, but here we just pass the name.
            isSubmission = series.getSelectedExamType().isSubmission();
        }

        return new StudentActiveCourseResponse(
                series.getId(),
                series.getModule() != null ? series.getModule().getName() : "Unknown",
                lecturerName,
                series.getStatus(),
                examTypeName,
                isSubmission,
                series.getSubmissionDeadline()
        );
    }
}
