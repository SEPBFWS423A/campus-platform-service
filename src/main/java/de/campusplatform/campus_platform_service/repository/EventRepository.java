package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE e.room.id = :roomId " +
           "AND e.startTime < :end " +
           "AND FUNCTION('DATEADD', MINUTE, e.durationMinutes, e.startTime) > :start " +
           "AND (:excludeId IS NULL OR e.id != :excludeId)")
    List<Event> findOverlappingEvents(@Param("roomId") Long roomId, 
                                     @Param("start") LocalDateTime start, 
                                     @Param("end") LocalDateTime end, 
                                     @Param("excludeId") Long excludeId);

    @Query("SELECT e.room.id FROM Event e WHERE e.room IS NOT NULL " +
           "AND e.startTime < :end " +
           "AND FUNCTION('DATEADD', MINUTE, e.durationMinutes, e.startTime) > :start " +
           "AND (:excludeId IS NULL OR e.id != :excludeId)")
    List<Long> findOccupiedRoomIds(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("excludeId") Long excludeId);

    @Query("SELECT e FROM Event e JOIN e.courseSeries cs WHERE cs.assignedLecturer.id = :lecturerId " +
           "AND e.startTime < :end " +
           "AND FUNCTION('DATEADD', MINUTE, e.durationMinutes, e.startTime) > :start " +
           "AND (:excludeId IS NULL OR e.id != :excludeId)")
    List<Event> findOverlappingEventsForLecturer(@Param("lecturerId") Long lecturerId, 
                                                @Param("start") LocalDateTime start, 
                                                @Param("end") LocalDateTime end, 
                                                @Param("excludeId") Long excludeId);
}
