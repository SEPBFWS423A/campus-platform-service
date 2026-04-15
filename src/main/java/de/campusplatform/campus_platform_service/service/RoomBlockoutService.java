package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.model.*;
import de.campusplatform.campus_platform_service.repository.CommunityEventRepository;
import de.campusplatform.campus_platform_service.repository.RoomBlockoutRepository;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomBlockoutService {

    private final RoomBlockoutRepository blockoutRepository;
    private final RoomRepository roomRepository;
    private final EventService eventService;
    private final CommunityEventRepository communityEventRepository;

    public BlockoutConflictResult checkConflicts(Long roomId, LocalDateTime start, LocalDateTime end) {
        List<RoomBlockoutResponse> overlaps = blockoutRepository.findActiveByRoomAndTimeOverlap(roomId, start, end)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
        
        var affectedEvents = eventService.getRoomSchedule(start, end).stream()
                .filter(e -> e.roomId() != null && e.roomId().equals(roomId))
                .collect(Collectors.toList());

        List<CommunityEvent> overlappingCommunityEvents = communityEventRepository.findOverlappingEvents(roomId, start, end, null);
        for (CommunityEvent ce : overlappingCommunityEvents) {
            affectedEvents.add(new RoomScheduleEventResponse(
                ce.getId(),
                ce.getTitle(),
                "COMMUNITY_EVENT",
                roomId,
                ce.getRoom() != null ? ce.getRoom().getName() : "Unknown",
                ce.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                (int) Duration.between(ce.getStartTime(), ce.getEndTime()).toMinutes(),
                null,
                ce.getCategory().name()
            ));
        }

        return new BlockoutConflictResult(overlaps, affectedEvents);
    }


    @Transactional
    public RoomBlockoutResponse createBlockout(RoomBlockoutRequest req, String username) {
        Room room = roomRepository.findById(req.roomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));

        RoomBlockout blockout = RoomBlockout.builder()
                .room(room)
                .startTime(req.startTime())
                .endTime(req.endTime())
                .reason(req.reason())
                .priority(req.priority())
                .notes(req.notes())
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build();

        RoomBlockout saved = blockoutRepository.save(blockout);
        return mapToResponse(saved);
    }

    public List<RoomBlockoutResponse> getBlockouts(Long roomId, Boolean active) {
        List<RoomBlockout> list;
        if (roomId != null) {
            list = blockoutRepository.findByRoomId(roomId);
        } else if (active != null && active) {
            list = blockoutRepository.findAllActive();
        } else {
            list = blockoutRepository.findAll();
        }

        return list.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoomBlockoutResponse resolveBlockout(Long id, String username) {
        RoomBlockout blockout = blockoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blockout not found"));
        
        blockout.setResolvedAt(LocalDateTime.now());
        blockout.setResolvedBy(username);
        
        return mapToResponse(blockoutRepository.save(blockout));
    }

    @Transactional
    public void deleteBlockout(Long id) {
        blockoutRepository.deleteById(id);
    }

    private RoomBlockoutResponse mapToResponse(RoomBlockout b) {
        return new RoomBlockoutResponse(
                b.getId(),
                b.getRoom().getId(),
                b.getRoom().getName(),
                b.getStartTime(),
                b.getEndTime(),
                b.getReason(),
                b.getPriority(),
                b.getNotes(),
                b.getCreatedBy(),
                b.getCreatedAt(),
                b.getResolvedAt() == null
        );
    }
}
