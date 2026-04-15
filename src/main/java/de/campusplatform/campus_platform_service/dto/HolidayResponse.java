package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.Holiday;
import java.time.LocalDate;

public class HolidayResponse {

    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String type;

    public HolidayResponse(Long id, String name, LocalDate startDate, LocalDate endDate, String type) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
    }

    public static HolidayResponse from(Holiday h) {
        return new HolidayResponse(
            h.getId(),
            h.getName(),
            h.getStartDate(),
            h.getEndDate(),
            h.getType().name().toLowerCase()
        );
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getType() { return type; }
}