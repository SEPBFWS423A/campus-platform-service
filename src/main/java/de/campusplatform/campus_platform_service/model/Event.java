package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.EventType;
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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "event_rooms",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "room_id")
    )
    private java.util.Set<Room> rooms = new java.util.HashSet<>();

    private String name;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private LocalDateTime startTime;
    private Integer durationMinutes;
}
