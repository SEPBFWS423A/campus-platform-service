package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        return ResponseEntity.ok(roomRepository.save(room));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room updated) {
        return roomRepository.findById(id).map(room -> {
            room.setName(updated.getName());
            room.setSeats(updated.getSeats());
            room.setExamSeats(updated.getExamSeats());
            return ResponseEntity.ok(roomRepository.save(room));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        roomRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
