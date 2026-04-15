package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AttendeeInfo;
import de.campusplatform.campus_platform_service.dto.CommunityEventRequest;
import de.campusplatform.campus_platform_service.dto.CommunityEventResponse;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.CommunityEvent;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.CommunityEventRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import de.campusplatform.campus_platform_service.repository.RoomBlockoutRepository;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityEventService {

    private final CommunityEventRepository communityEventRepository;
    private final EventRepository eventRepository;
    private final RoomBlockoutRepository roomBlockoutRepository;
    private final RoomRepository roomRepository;
    private final AppUserRepository appUserRepository;

    public CommunityEventService(CommunityEventRepository communityEventRepository,
                                 EventRepository eventRepository,
                                 RoomBlockoutRepository roomBlockoutRepository,
                                 RoomRepository roomRepository,
                                 AppUserRepository appUserRepository) {
        this.communityEventRepository = communityEventRepository;
        this.eventRepository = eventRepository;
        this.roomBlockoutRepository = roomBlockoutRepository;
        this.roomRepository = roomRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<CommunityEventResponse> getAllFutureEvents() {
        return communityEventRepository.findAllFutureEvents(LocalDateTime.now())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommunityEventResponse createEvent(CommunityEventRequest request, String username) {
        AppUser creator = appUserRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CommunityEvent event = new CommunityEvent();
        event.setCreator(creator);
        mapRequestToEntity(request, event);

        if (event.getRoom() != null) {
            validateRoomAvailability(event.getRoom().getId(), event.getStartTime(), event.getEndTime(), null);
        }

        CommunityEvent saved = communityEventRepository.save(event);
        return mapToResponse(saved);
    }

    @Transactional
    public CommunityEventResponse updateEvent(Long id, CommunityEventRequest request, String username) {
        CommunityEvent event = communityEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!event.getCreator().getEmail().equalsIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit your own events");
        }

        mapRequestToEntity(request, event);

        if (event.getRoom() != null) {
            validateRoomAvailability(event.getRoom().getId(), event.getStartTime(), event.getEndTime(), id);
        }

        CommunityEvent saved = communityEventRepository.save(event);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteEvent(Long id, String username) {
        CommunityEvent event = communityEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!event.getCreator().getEmail().equalsIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own events");
        }

        communityEventRepository.delete(event);
    }

    @Transactional
    public void rsvp(Long id, String username) {
        CommunityEvent event = communityEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        AppUser user = appUserRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (event.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creator cannot RSVP to their own event");
        }

        event.getAttendees().add(user);
        communityEventRepository.save(event);
    }

    @Transactional
    public void cancelRsvp(Long id, String username) {
        CommunityEvent event = communityEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        AppUser user = appUserRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        event.getAttendees().remove(user);
        communityEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms(LocalDateTime start, LocalDateTime end, Long excludeId) {
        if (start == null || end == null) {
            return roomRepository.findAll();
        }

        return roomRepository.findAll().stream()
                .filter(room -> isRoomAvailable(room.getId(), start, end, excludeId))
                .collect(Collectors.toList());
    }

    private boolean isRoomAvailable(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        // 1. Check Academic Events
        if (!eventRepository.findOverlappingEvents(Collections.singletonList(roomId), start, end, null).isEmpty()) {
            return false;
        }

        // 2. Check Room Blockouts
        if (!roomBlockoutRepository.findActiveByRoomAndTimeOverlap(roomId, start, end).isEmpty()) {
            return false;
        }

        // 3. Check Other Community Events
        return communityEventRepository.findOverlappingEvents(roomId, start, end, excludeId).isEmpty();
    }

    private void validateRoomAvailability(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        // 1. Check Academic Events
        if (!eventRepository.findOverlappingEvents(Collections.singletonList(roomId), start, end, null).isEmpty()) {
            throw new AppException("Room is occupied by a lecture or exam");
        }

        // 2. Check Room Blockouts
        if (!roomBlockoutRepository.findActiveByRoomAndTimeOverlap(roomId, start, end).isEmpty()) {
            throw new AppException("Room is currently blocked for maintenance or other reasons");
        }

        // 3. Check Other Community Events
        if (!communityEventRepository.findOverlappingEvents(roomId, start, end, excludeId).isEmpty()) {
            throw new AppException("Room is already booked for another student event");
        }
    }

    private void mapRequestToEntity(CommunityEventRequest request, CommunityEvent entity) {
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setCategory(request.category());
        entity.setCustomLocation(request.customLocation());

        if (request.roomId() != null) {
            Room room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
            entity.setRoom(room);
        } else {
            entity.setRoom(null);
        }
    }

    private CommunityEventResponse mapToResponse(CommunityEvent entity) {
        List<AttendeeInfo> attendees = entity.getAttendees().stream()
                .map(user -> new AttendeeInfo(user.getId(), user.getFirstName() + " " + user.getLastName()))
                .collect(Collectors.toList());

        return new CommunityEventResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getCategory(),
                entity.getCreator().getId(),
                entity.getCreator().getFirstName() + " " + entity.getCreator().getLastName(),
                entity.getRoom() != null ? entity.getRoom().getId() : null,
                entity.getRoom() != null ? entity.getRoom().getName() : null,
                entity.getCustomLocation(),
                attendees
        );
    }
}
