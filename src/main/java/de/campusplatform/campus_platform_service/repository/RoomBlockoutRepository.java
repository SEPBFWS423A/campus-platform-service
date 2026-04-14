package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.RoomBlockout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RoomBlockoutRepository extends JpaRepository<RoomBlockout, Long> {
    
    @Query("SELECT b FROM RoomBlockout b WHERE b.room.id = :roomId " +
           "AND b.resolvedAt IS NULL " +
           "AND b.startTime < :end AND b.endTime > :start")
    List<RoomBlockout> findActiveByRoomAndTimeOverlap(Long roomId, LocalDateTime start, LocalDateTime end);

    List<RoomBlockout> findByRoomId(Long roomId);

    @Query("SELECT b FROM RoomBlockout b WHERE b.resolvedAt IS NULL AND b.endTime > CURRENT_TIMESTAMP")
    List<RoomBlockout> findAllActive();
}
