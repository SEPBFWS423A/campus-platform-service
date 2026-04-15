package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.CommunityEventRequest;
import de.campusplatform.campus_platform_service.dto.CommunityEventResponse;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import de.campusplatform.campus_platform_service.service.CommunityEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/social/events")
public class SocialEventController {

    private final CommunityEventService communityEventService;
    private final RoomRepository roomRepository;

    public SocialEventController(CommunityEventService communityEventService, RoomRepository roomRepository) {
        this.communityEventService = communityEventService;
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public ResponseEntity<List<CommunityEventResponse>> getEvents() {
        return ResponseEntity.ok(communityEventService.getAllFutureEvents());
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getRooms(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime start,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime end,
            @RequestParam(required = false) Long excludeId) {
        return ResponseEntity.ok(communityEventService.getAvailableRooms(start, end, excludeId));
    }


    @PostMapping
    public ResponseEntity<CommunityEventResponse> createEvent(@RequestBody CommunityEventRequest request, Principal principal) {
        return ResponseEntity.status(201).body(communityEventService.createEvent(request, principal.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommunityEventResponse> updateEvent(@PathVariable Long id, @RequestBody CommunityEventRequest request, Principal principal) {
        return ResponseEntity.ok(communityEventService.updateEvent(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id, Principal principal) {
        communityEventService.deleteEvent(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/rsvp")
    public ResponseEntity<Void> rsvpToEvent(@PathVariable Long id, Principal principal) {
        communityEventService.rsvp(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/rsvp")
    public ResponseEntity<Void> cancelRsvpToEvent(@PathVariable Long id, Principal principal) {
        communityEventService.cancelRsvp(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
