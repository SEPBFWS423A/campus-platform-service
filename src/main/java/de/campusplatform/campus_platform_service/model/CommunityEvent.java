package de.campusplatform.campus_platform_service.model;

import de.campusplatform.campus_platform_service.enums.StudentEventCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "community_event")
public class CommunityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentEventCategory category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id")
    private AppUser creator;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "custom_location")
    private String customLocation;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "community_event_attendees",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private java.util.Set<AppUser> attendees = new java.util.HashSet<>();
}
