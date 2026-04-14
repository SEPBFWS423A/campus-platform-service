package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.RoomStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomStatusHistoryRepository extends JpaRepository<RoomStatusHistory, Long> {
    List<RoomStatusHistory> findByRoomIdOrderByChangedAtDesc(Long roomId);
}
