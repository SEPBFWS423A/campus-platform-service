package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.CommunityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommunityEventRepository extends JpaRepository<CommunityEvent, Long> {

    @Query("SELECT e FROM CommunityEvent e WHERE e.room.id = :roomId " +
           "AND e.startTime < :end " +
           "AND e.endTime > :start " +
           "AND (:excludeId IS NULL OR e.id != :excludeId)")
    List<CommunityEvent> findOverlappingEvents(@Param("roomId") Long roomId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("excludeId") Long excludeId);

    List<CommunityEvent> findByCreatorId(Long id);

    @Query("SELECT e FROM CommunityEvent e WHERE e.startTime >= :now ORDER BY e.startTime ASC")
    List<CommunityEvent> findAllFutureEvents(@Param("now") LocalDateTime now);
}
