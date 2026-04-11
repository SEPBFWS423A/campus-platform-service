package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AdminEventResponse;
import de.campusplatform.campus_platform_service.dto.EventRequest;
import de.campusplatform.campus_platform_service.enums.EventType;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.CourseSeries;
import de.campusplatform.campus_platform_service.model.Event;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.CourseSeriesRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import de.campusplatform.campus_platform_service.enums.EventType;
import de.campusplatform.campus_platform_service.model.ExamType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CourseSeriesRepository courseSeriesRepository;
    private final RoomRepository roomRepository;
    private final StudentSubmissionService studentSubmissionService;

    public EventService(EventRepository eventRepository, 
                        CourseSeriesRepository courseSeriesRepository, 
                        RoomRepository roomRepository,
                        StudentSubmissionService studentSubmissionService) {
        this.eventRepository = eventRepository;
        this.courseSeriesRepository = courseSeriesRepository;
        this.roomRepository = roomRepository;
        this.studentSubmissionService = studentSubmissionService;
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
        
        Event saved = eventRepository.save(event);
        
        return mapToAdminResponse(saved);
    }

    @Transactional
    public AdminEventResponse updateEvent(Long eventId, EventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException("Event not found"));
        
        mapRequestToEntity(request, event);
        
        validateNoCollision(event, eventId);
        
        Event saved = eventRepository.save(event);
        
        ExamType examType = saved.getCourseSeries().getSelectedExamType();
        if (examType == null && saved.getCourseSeries().getModule() != null) {
            examType = saved.getCourseSeries().getModule().getPreferredExamType();
        }

        if (saved.getEventType() == EventType.KLAUSUR) {
            // No longer doing anything special here for WRITTEN exams
        } else if (examType == null || !examType.isSubmission()) {
            // In case the type was changed FROM KLAUSUR to something else for WRITTEN exams
            studentSubmissionService.cleanupSubmissionsIfNoKlausurExists(saved.getCourseSeries().getId());
        }
        
        return mapToAdminResponse(saved);
    }

    public AdminEventResponse fastAddEvent(Long seriesId) {
        CourseSeries series = courseSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException("Course Series not found"));

        Set<Event> existingEvents = series.getEvents();
        Event template = existingEvents != null && !existingEvents.isEmpty()
                ? existingEvents.stream()
                    .filter(e -> e.getStartTime() != null)
                    .max(Comparator.comparing(Event::getStartTime))
                    .orElse(null)
                : null;

        LocalDateTime nextStart;
        if (template != null) {
            nextStart = addBusinessDays(template.getStartTime(), 3);
        } else {
            nextStart = addBusinessDays(LocalDateTime.now().withHour(8).withMinute(0).withSecond(0).withNano(0), 3);
        }

        Event newEvent = new Event();
        newEvent.setCourseSeries(series);
        if (template != null) {
            newEvent.setName(series.getModule().getName() + " (" + (existingEvents.size() + 1) + ")");
            newEvent.setEventType(template.getEventType());
            newEvent.setDurationMinutes(template.getDurationMinutes());
            if (template.getRooms() != null) {
                newEvent.setRooms(new java.util.HashSet<>(template.getRooms()));
            }
        } else {
            newEvent.setName(series.getModule().getName() + " (1)");
            newEvent.setEventType(EventType.LEHRVERANSTALTUNG);
            newEvent.setDurationMinutes(90);
        }

        // Search for next available slot
        while (hasCollision(newEvent, nextStart)) {
            nextStart = addBusinessDays(nextStart, 1);
        }
        newEvent.setStartTime(nextStart);

        // Smart Room Assignment
        int requiredSeats = calculateRequiredSeats(series);
        EventType eventType = newEvent.getEventType();
        
        // Try template rooms first
        Set<Room> selectedRooms = new java.util.HashSet<>();
        if (template != null && template.getRooms() != null) {
            for (Room room : template.getRooms()) {
                if (isRoomAvailable(room, nextStart, newEvent.getDurationMinutes(), null) && 
                    hasCapacity(room, requiredSeats, eventType)) {
                    selectedRooms.add(room);
                }
            }
        }

        // If no rooms selected yet, try to find any available room with enough capacity
        if (selectedRooms.isEmpty()) {
            List<Room> availableRooms = getAvailableRooms(nextStart, newEvent.getDurationMinutes(), null, seriesId, eventType.name());
            if (!availableRooms.isEmpty()) {
                // Pick the first one that fits
                selectedRooms.add(availableRooms.get(0));
            }
        }
        
        newEvent.setRooms(selectedRooms);
        Event saved = eventRepository.save(newEvent);
        return mapToAdminResponse(saved);
    }

    private int calculateRequiredSeats(CourseSeries series) {
        if (series.getStudyGroups() == null) return 0;
        return series.getStudyGroups().stream()
                .flatMap(sg -> sg.getMemberships().stream())
                .map(m -> m.getStudent().getUserId())
                .collect(Collectors.toSet())
                .size();
    }

    private boolean hasCapacity(Room room, int requiredSeats, EventType eventType) {
        if (eventType == EventType.KLAUSUR) {
            return room.getExamSeats() != null && room.getExamSeats() >= requiredSeats;
        }
        return room.getSeats() != null && room.getSeats() >= requiredSeats;
    }

    private boolean isRoomAvailable(Room room, LocalDateTime start, Integer duration, Long excludeId) {
        LocalDateTime end = start.plusMinutes(duration);
        List<Event> overlaps = eventRepository.findOverlappingEvents(java.util.Collections.singletonList(room.getId()), start, end, excludeId);
        return overlaps.isEmpty();
    }

    private LocalDateTime addBusinessDays(LocalDateTime start, int days) {
        LocalDateTime result = start;
        int added = 0;
        while (added < days) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }

    private boolean hasCollision(Event newEvent, LocalDateTime proposedStart) {
        if (newEvent.getRooms() == null || newEvent.getRooms().isEmpty() || proposedStart == null || newEvent.getDurationMinutes() == null) {
            return false;
        }

        LocalDateTime end = proposedStart.plusMinutes(newEvent.getDurationMinutes());
        List<Long> roomIds = newEvent.getRooms().stream().map(Room::getId).collect(Collectors.toList());

        // 1. Room Collision
        List<Event> overlaps = eventRepository.findOverlappingEvents(roomIds, proposedStart, end, null);
        if (!overlaps.isEmpty()) {
            return true;
        }

        // 2. Lecturer Collision
        if (newEvent.getCourseSeries() != null && newEvent.getCourseSeries().getAssignedLecturer() != null) {
            Long lecturerId = newEvent.getCourseSeries().getAssignedLecturer().getId();
            List<Event> lecturerOverlaps = eventRepository.findOverlappingEventsForLecturer(lecturerId, proposedStart, end, null);
            if (!lecturerOverlaps.isEmpty()) return true;
        }

        // 3. Study Group Collision
        if (newEvent.getCourseSeries() != null && newEvent.getCourseSeries().getStudyGroups() != null) {
            List<Long> groupIds = newEvent.getCourseSeries().getStudyGroups().stream()
                    .map(de.campusplatform.campus_platform_service.model.StudyGroup::getId)
                    .collect(Collectors.toList());
            if (!groupIds.isEmpty()) {
                List<Event> groupOverlaps = eventRepository.findOverlappingEventsForGroups(groupIds, proposedStart, end, null);
                return !groupOverlaps.isEmpty();
            }
        }

        return false;
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException("Event not found"));
        
        Long seriesId = event.getCourseSeries() != null ? event.getCourseSeries().getId() : null;
        EventType type = event.getEventType();
        
        eventRepository.delete(event);
        
        if (type == EventType.KLAUSUR && seriesId != null) {
            studentSubmissionService.cleanupSubmissionsIfNoKlausurExists(seriesId);
        }
    }

    private void mapRequestToEntity(EventRequest request, Event entity) {
        if (entity.getRooms() == null) {
            entity.setRooms(new java.util.HashSet<>());
        } else {
            entity.getRooms().clear();
        }

        if (request.roomIds() != null && !request.roomIds().isEmpty()) {
            List<Room> rooms = roomRepository.findAllById(request.roomIds());
            entity.getRooms().addAll(rooms);
        }
        entity.setName(request.name());
        entity.setEventType(request.eventType());
        entity.setStartTime(request.startTime());
        entity.setDurationMinutes(request.durationMinutes());
    }

    public List<Room> getAvailableRooms(LocalDateTime startTime, Integer durationMinutes, Long excludeEventId, Long seriesId, String eventTypeStr) {
        List<Room> allRooms = roomRepository.findAll();
        if (startTime == null || durationMinutes == null) {
            return allRooms;
        }
        
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        List<Long> occupiedRoomIds = eventRepository.findOccupiedRoomIds(startTime, endTime, excludeEventId);
        
        int requiredSeats = 0;
        EventType eventType = null;
        if (seriesId != null) {
            CourseSeries series = courseSeriesRepository.findById(seriesId).orElse(null);
            if (series != null) {
                requiredSeats = calculateRequiredSeats(series);
            }
        }
        if (eventTypeStr != null) {
            try {
                eventType = EventType.valueOf(eventTypeStr);
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }

        final int finalRequiredSeats = requiredSeats;
        final EventType finalEventType = eventType;

        return allRooms.stream()
                .filter(room -> !occupiedRoomIds.contains(room.getId()))
                .filter(room -> finalRequiredSeats <= 0 || hasCapacity(room, finalRequiredSeats, finalEventType))
                .collect(Collectors.toList());
    }

    private void validateNoCollision(Event event, Long excludeId) {
        if (event.getRooms() == null || event.getRooms().isEmpty() || event.getStartTime() == null || event.getDurationMinutes() == null) {
            return;
        }

        LocalDateTime end = event.getStartTime().plusMinutes(event.getDurationMinutes());
        List<Long> roomIds = event.getRooms().stream().map(Room::getId).collect(Collectors.toList());
        
        List<Event> overlaps = eventRepository.findOverlappingEvents(
                roomIds,
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

        // 3. Study Group Collision
        if (event.getCourseSeries() != null && event.getCourseSeries().getStudyGroups() != null) {
            List<Long> groupIds = event.getCourseSeries().getStudyGroups().stream()
                    .map(de.campusplatform.campus_platform_service.model.StudyGroup::getId)
                    .collect(Collectors.toList());
            if (!groupIds.isEmpty()) {
                List<Event> groupOverlaps = eventRepository.findOverlappingEventsForGroups(
                        groupIds,
                        event.getStartTime(),
                        end,
                        excludeId
                );

                if (!groupOverlaps.isEmpty()) {
                    throw new AppException("eventManagement.groupCollision");
                }
            }
        }
    }

    private AdminEventResponse mapToAdminResponse(Event entity) {
        List<AdminEventResponse.RoomResponse> roomResponses = entity.getRooms() != null
                ? entity.getRooms().stream()
                    .map(r -> new AdminEventResponse.RoomResponse(r.getId(), r.getName()))
                    .collect(Collectors.toList())
                : List.of();

        return new AdminEventResponse(
                entity.getId(),
                entity.getCourseSeries() != null ? entity.getCourseSeries().getId() : null,
                roomResponses,
                entity.getName(),
                entity.getEventType(),
                entity.getStartTime(),
                entity.getDurationMinutes()
        );
    }
}
