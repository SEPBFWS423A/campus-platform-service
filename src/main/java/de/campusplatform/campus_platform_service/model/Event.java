package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "event")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_series_id")
    private CourseSeries courseSeries;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    private String name;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private LocalDateTime startTime;
    private Integer durationMinutes;
}
