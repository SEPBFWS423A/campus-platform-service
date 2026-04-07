package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AdminEventResponse;
import de.campusplatform.campus_platform_service.dto.EventRequest;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.CourseSeries;
import de.campusplatform.campus_platform_service.model.Event;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.CourseSeriesRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CourseSeriesRepository courseSeriesRepository;
    private final RoomRepository roomRepository;

    public EventService(EventRepository eventRepository, CourseSeriesRepository courseSeriesRepository, RoomRepository roomRepository) {
        this.eventRepository = eventRepository;
        this.courseSeriesRepository = courseSeriesRepository;
        this.roomRepository = roomRepository;
    }

    public List<AdminEventResponse> getEventsForSeries(Long seriesId) {
        CourseSeries series = courseSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException("Course Series not found"));
        
        // Return emtpy list if null to be safe
        if(series.getEvents() == null) {
            return List.of();
        }
        
        return series.getEvents().stream()
                .map(this::mapToAdminResponse)
                .collect(Collectors.toList());
    }

    public AdminEventResponse createEvent(Long seriesId, EventRequest request) {
        CourseSeries series = courseSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException("Course Series not found"));
        
        Event event = new Event();
        event.setCourseSeries(series);
        mapRequestToEntity(request, event);
        
        validateNoCollision(event, null);
        
        Event saved = eventRepository.save(event);
        return mapToAdminResponse(saved);
    }

    public AdminEventResponse updateEvent(Long eventId, EventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException("Event not found"));
        
        mapRequestToEntity(request, event);
        
        validateNoCollision(event, eventId);
        
        Event saved = eventRepository.save(event);
        return mapToAdminResponse(saved);
    }

    public void deleteEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new AppException("Event not found");
        }
        eventRepository.deleteById(eventId);
    }

    private void mapRequestToEntity(EventRequest request, Event entity) {
        Room room = null;
        if (request.roomId() != null) {
            room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new AppException("Room not found"));
        }
        entity.setRoom(room);
        entity.setName(request.name());
        entity.setEventType(request.eventType());
        entity.setStartTime(request.startTime());
        entity.setDurationMinutes(request.durationMinutes());
    }

    public List<Room> getAvailableRooms(LocalDateTime startTime, Integer durationMinutes, Long excludeEventId) {
        if (startTime == null || durationMinutes == null) {
            return roomRepository.findAll();
        }
        
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        List<Long> occupiedRoomIds = eventRepository.findOccupiedRoomIds(startTime, endTime, excludeEventId);
        
        return roomRepository.findAll().stream()
                .filter(room -> !occupiedRoomIds.contains(room.getId()))
                .collect(Collectors.toList());
    }

    private void validateNoCollision(Event event, Long excludeId) {
        if (event.getRoom() == null || event.getStartTime() == null || event.getDurationMinutes() == null) {
            return;
        }

        LocalDateTime end = event.getStartTime().plusMinutes(event.getDurationMinutes());
        List<Event> overlaps = eventRepository.findOverlappingEvents(
                event.getRoom().getId(),
                event.getStartTime(),
                end,
                excludeId
        );

        if (!overlaps.isEmpty()) {
            throw new AppException("eventManagement.roomCollision");
        }

        // 2. Lecturer Collision
        if (event.getCourseSeries() != null && event.getCourseSeries().getAssignedLecturer() != null) {
            Long lecturerId = event.getCourseSeries().getAssignedLecturer().getId();
            List<Event> lecturerOverlaps = eventRepository.findOverlappingEventsForLecturer(
                    lecturerId,
                    event.getStartTime(),
                    end,
                    excludeId
            );

            if (!lecturerOverlaps.isEmpty()) {
                throw new AppException("eventManagement.lecturerCollision");
            }
        }
    }

    private AdminEventResponse mapToAdminResponse(Event entity) {
        return new AdminEventResponse(
                entity.getId(),
                entity.getCourseSeries() != null ? entity.getCourseSeries().getId() : null,
                entity.getRoom() != null ? entity.getRoom().getId() : null,
                entity.getRoom() != null ? entity.getRoom().getName() : null,
                entity.getName(),
                entity.getEventType(),
                entity.getStartTime(),
                entity.getDurationMinutes()
        );
    }
}
