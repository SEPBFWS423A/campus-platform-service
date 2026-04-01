package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.AdminUserResponse;
import de.campusplatform.campus_platform_service.dto.AdminUserUpdateRequest;
import de.campusplatform.campus_platform_service.dto.InvitationRequest;
import de.campusplatform.campus_platform_service.dto.UserStatsResponse;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import de.campusplatform.campus_platform_service.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AuthService authService;
    private final RoomRepository roomRepository;

    public AdminController(AuthService authService, RoomRepository roomRepository) {
        this.authService = authService;
        this.roomRepository = roomRepository;
    }

    @PostMapping("/invite")
    public ResponseEntity<Void> inviteUser(@RequestBody InvitationRequest request) {
        authService.inviteUser(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/users/stats")
    public ResponseEntity<UserStatsResponse> getUserStats() {
        return ResponseEntity.ok(authService.getUserStats());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable Long id, @RequestBody AdminUserUpdateRequest request) {
        authService.updateUser(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        return ResponseEntity.ok(roomRepository.save(room));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room updated) {
        return roomRepository.findById(id).map(room -> {
            room.setName(updated.getName());
            room.setSeats(updated.getSeats());
            room.setExamSeats(updated.getExamSeats());
            return ResponseEntity.ok(roomRepository.save(room));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        roomRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
