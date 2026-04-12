package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("SELECT DISTINCT e FROM Event e JOIN e.rooms r WHERE r.id IN :roomIds " +
           "AND e.startTime < :end " +
           "AND FUNCTION('DATEADD', MINUTE, e.durationMinutes, e.startTime) > :start " +
           "AND (:excludeId IS NULL OR e.id != :excludeId)")
    List<Event> findOverlappingEvents(@Param("roomIds") Collection<Long> roomIds,
                                     @Param("start") LocalDateTime start, 
                                     @Param("end") LocalDateTime end, 
                                     @Param("excludeId") Long excludeId);

    @Query("SELECT DISTINCT r.id FROM Event e JOIN e.rooms r WHERE " +
           "e.startTime < :end " +
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

    @Query("SELECT DISTINCT e FROM Event e JOIN e.courseSeries cs JOIN cs.studyGroups sg WHERE sg.id IN :groupIds " +
           "AND e.startTime < :end " +
           "AND FUNCTION('DATEADD', MINUTE, e.durationMinutes, e.startTime) > :start " +
           "AND (:excludeId IS NULL OR e.id != :excludeId)")
    List<Event> findOverlappingEventsForGroups(@Param("groupIds") Collection<Long> groupIds, 
                                              @Param("start") LocalDateTime start, 
                                              @Param("end") LocalDateTime end, 
                                              @Param("excludeId") Long excludeId);
  
    boolean existsByCourseSeriesIdAndEventType(Long courseSeriesId, de.campusplatform.campus_platform_service.enums.EventType eventType);

    @Query("SELECT DISTINCT e FROM Event e " +
           "JOIN e.courseSeries cs " +
           "JOIN cs.studyGroups sg " +
           "JOIN sg.memberships m " +
           "WHERE m.student.userId = :userId " +
           "AND e.startTime >= :startTime " +
           "ORDER BY e.startTime ASC")
    List<Event> findUpcomingEventsByStudentUserId(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}
