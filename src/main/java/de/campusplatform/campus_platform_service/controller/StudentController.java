package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.StudentActiveCourseResponse;
import de.campusplatform.campus_platform_service.dto.StudentEventResponse;
import de.campusplatform.campus_platform_service.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/timetable")
@PreAuthorize("hasAuthority('STUDENT')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<StudentEventResponse>> getUpcomingEvents(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(studentService.getUpcomingEvents(userDetails.getUsername()));
    }

    @GetMapping("/active-series")
    public ResponseEntity<List<StudentActiveCourseResponse>> getActiveCourseSeries(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(studentService.getActiveCourseSeries(userDetails.getUsername()));
    }
}
