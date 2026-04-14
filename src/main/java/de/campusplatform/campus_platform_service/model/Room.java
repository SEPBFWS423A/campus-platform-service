package de.campusplatform.campus_platform_service.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "room")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer seats;

    @Column(nullable = false)
    private Integer examSeats;

    @Column(nullable = false)
    private String building;

    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OperationalStatus operationalStatus = OperationalStatus.AKTIV;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_features", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "feature")
    @Builder.Default
    private java.util.Set<String> features = new java.util.HashSet<>();

    @Builder.Default
    private Boolean barrierefreiheit = false;

    @Column(length = 500)
    private String description;
}
