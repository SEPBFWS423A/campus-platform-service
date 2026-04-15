package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Application;
import java.time.LocalDateTime;

public class ApplicationResponse {

    private Long id;
    private String programName;
    private String status;
    private Integer priority;
    private String motivation;
    private LocalDateTime createdAt;

    public ApplicationResponse(Long id, String programName, String status,
                               Integer priority, String motivation, LocalDateTime createdAt) {
        this.id = id;
        this.programName = programName;
        this.status = status;
        this.priority = priority;
        this.motivation = motivation;
        this.createdAt = createdAt;
    }

    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
            a.getId(),
            a.getProgram().getName(),
            a.getStatus().name(),
            a.getPriority(),
            a.getMotivation(),
            a.getCreatedAt()
        );
    }

    public Long getId() { return id; }
    public String getProgramName() { return programName; }
    public String getStatus() { return status; }
    public Integer getPriority() { return priority; }
    public String getMotivation() { return motivation; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}