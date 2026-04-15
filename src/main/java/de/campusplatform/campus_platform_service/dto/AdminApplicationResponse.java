package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Application;
import java.time.LocalDateTime;

public class AdminApplicationResponse {

    private Long id;
    private String studentName;
    private String studentEmail;
    private String programName;
    private String status;
    private Integer priority;
    private String motivation;
    private LocalDateTime createdAt;

    public AdminApplicationResponse(Long id, String studentName, String studentEmail,
                                    String programName, String status, Integer priority,
                                    String motivation, LocalDateTime createdAt) {
        this.id = id;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.programName = programName;
        this.status = status;
        this.priority = priority;
        this.motivation = motivation;
        this.createdAt = createdAt;
    }

    public static AdminApplicationResponse from(Application a) {
        return new AdminApplicationResponse(
            a.getId(),
            a.getStudent().getFirstName() + " " + a.getStudent().getLastName(),
            a.getStudent().getEmail(),
            a.getProgram().getName(),
            a.getStatus().name(),
            a.getPriority(),
            a.getMotivation(),
            a.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getProgramName() { return programName; }
    public String getStatus() { return status; }
    public Integer getPriority() { return priority; }
    public String getMotivation() { return motivation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}