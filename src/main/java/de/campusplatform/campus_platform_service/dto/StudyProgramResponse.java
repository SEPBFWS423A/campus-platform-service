package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.StudyProgram;

public class StudyProgramResponse {

    private Long id;
    private String name;
    private String description;

    public StudyProgramResponse(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public static StudyProgramResponse from(StudyProgram p) {
        return new StudyProgramResponse(p.getId(), p.getName(), p.getDescription());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}