package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AdminEventResponse;
import de.campusplatform.campus_platform_service.dto.AutoScheduleRequest;
import de.campusplatform.campus_platform_service.dto.EventRequest;
import de.campusplatform.campus_platform_service.repository.CommunityEventRepository;
import de.campusplatform.campus_platform_service.dto.RoomScheduleEventResponse;
import de.campusplatform.campus_platform_service.dto.RoomUtilizationResponse;
import de.campusplatform.campus_platform_service.enums.EventType;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.CourseSeries;
import de.campusplatform.campus_platform_service.model.Event;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.CourseSeriesRepository;
import de.campusplatform.campus_platform_service.repository.EventRepository;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import de.campusplatform.campus_platform_service.model.ExamType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CourseSeriesRepository courseSeriesRepository;
    private final RoomRepository roomRepository;
    private final StudentSubmissionService studentSubmissionService;
    private final LecturerAbsenceService lecturerAbsenceService;
    private final CommunityEventRepository communityEventRepository;

    public EventService(EventRepository eventRepository, 
                        CourseSeriesRepository courseSeriesRepository, 
                        RoomRepository roomRepository,
                        StudentSubmissionService studentSubmissionService,
                        LecturerAbsenceService lecturerAbsenceService,
                        CommunityEventRepository communityEventRepository) {
        this.eventRepository = eventRepository;
        this.courseSeriesRepository = courseSeriesRepository;
        this.roomRepository = roomRepository;
        this.studentSubmissionService = studentSubmissionService;
        this.lecturerAbsenceService = lecturerAbsenceService;
        this.communityEventRepository = communityEventRepository;
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
            newEvent.setDurationMinutes(195);
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
        if (proposedStart == null || newEvent.getDurationMinutes() == null) {
            return false;
        }

        LocalDateTime end = proposedStart.plusMinutes(newEvent.getDurationMinutes());

        // 1. Room Collision (only if rooms are assigned)
        if (newEvent.getRooms() != null && !newEvent.getRooms().isEmpty()) {
            List<Long> roomIds = newEvent.getRooms().stream().map(Room::getId).collect(Collectors.toList());
            List<Event> overlaps = eventRepository.findOverlappingEvents(roomIds, proposedStart, end, null);
            if (!overlaps.isEmpty()) {
                return true;
            }
            
            for (Long roomId : roomIds) {
                if (!communityEventRepository.findOverlappingEvents(roomId, proposedStart, end, null).isEmpty()) {
                    return true;
                }
            }
        }

        // 2. Lecturer Collision
        if (newEvent.getCourseSeries() != null && newEvent.getCourseSeries().getAssignedLecturer() != null) {
            Long lecturerId = newEvent.getCourseSeries().getAssignedLecturer().getId();

            if (lecturerAbsenceService.hasAbsenceConflict(lecturerId, proposedStart, end)) {
                return true;
            }

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
    public void autoSchedule(Long seriesId, AutoScheduleRequest request) {
        CourseSeries series = courseSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new AppException("Course Series not found"));
        
        int requiredMinutes = series.getModule().getRequiredTotalHours() * 60;
        int currentMinutes = series.getEvents().stream().mapToInt(Event::getDurationMinutes).sum();
        int deficitMinutes = requiredMinutes - currentMinutes;
        
        if (deficitMinutes <= 0) return;
        
        // 1. Identify all working days in range
        List<LocalDate> workingDays = new ArrayList<>();
        LocalDate current = request.startDate();
        while (!current.isAfter(request.endDate())) {
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY && current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays.add(current);
            }
            current = current.plusDays(1);
        }
        
        if (workingDays.isEmpty()) return;
        
        // 2. Prepare prioritized slots
        record ParsedSlot(LocalTime start, LocalTime end, int duration) {}
        List<ParsedSlot> slots = request.timeSlots().stream()
                .map(ts -> {
                    LocalTime s = LocalTime.parse(ts.startTime());
                    LocalTime e = LocalTime.parse(ts.endTime());
                    return new ParsedSlot(s, e, (int) Duration.between(s, e).toMinutes());
                })
                .collect(Collectors.toList());
        
        double avgDuration = slots.stream().mapToDouble(s -> s.duration).average().orElse(90.0);
        int approxEventsNeeded = (int) Math.ceil((double) deficitMinutes / avgDuration);
        double dayStep = (double) workingDays.size() / approxEventsNeeded;
        
        Set<LocalDate> occupiedDays = new HashSet<>();
        List<Room> availableCandidateRooms = roomRepository.findAll();
        int requiredSeats = calculateRequiredSeats(series);
        int addedMinutes = 0;
        
        // 3. Spaced pass
        for (int i = 0; i < approxEventsNeeded && (currentMinutes + addedMinutes) < requiredMinutes; i++) {
            int targetIdx = (int) Math.floor(i * dayStep);
            boolean scheduled = false;
            
            for (int k = 0; k < workingDays.size() && !scheduled; k++) {
                // Search around targetIdx: +0, +1, -1, +2, -2...
                int offset = (k % 2 == 0) ? k / 2 : -(k / 2 + 1);
                int idx = targetIdx + offset;
                
                if (idx < 0 || idx >= workingDays.size()) continue;
                
                LocalDate date = workingDays.get(idx);
                if (occupiedDays.contains(date)) continue;
                
                // Try slots in priority order
                for (ParsedSlot slot : slots) {
                    LocalDateTime proposedStart = date.atTime(slot.start);
                    
                    // a) Initial collision check (lecturer/groups)
                    Event tempEvent = new Event();
                    tempEvent.setCourseSeries(series);
                    tempEvent.setDurationMinutes(slot.duration);
                    
                    if (!hasCollision(tempEvent, proposedStart)) {
                        // b) Room finding
                        Room selectedRoom = null;
                        for (Room room : availableCandidateRooms) {
                            if ((room.getOperationalStatus() == de.campusplatform.campus_platform_service.model.OperationalStatus.AKTIV || 
                                 room.getOperationalStatus() == de.campusplatform.campus_platform_service.model.OperationalStatus.EINGESCHRAENKT) &&
                                hasCapacity(room, requiredSeats, EventType.LEHRVERANSTALTUNG) && 
                                isRoomAvailable(room, proposedStart, slot.duration, null)) {
                                selectedRoom = room;
                                break;
                            }
                        }
                        
                        if (selectedRoom != null) {
                            // Found a valid slot!
                            Event newEvent = new Event();
                            newEvent.setCourseSeries(series);
                            newEvent.setName(series.getModule().getName() + " (" + (series.getEvents().size() + 1) + ")");
                            newEvent.setEventType(EventType.LEHRVERANSTALTUNG);
                            newEvent.setStartTime(proposedStart);
                            newEvent.setDurationMinutes(slot.duration);
                            newEvent.setRooms(Set.of(selectedRoom));
                            
                            eventRepository.save(newEvent);
                            series.getEvents().add(newEvent); // Keep track locally for name counting and hours
                            addedMinutes += slot.duration;
                            occupiedDays.add(date);
                            scheduled = true;
                            break;
                        }
                    }
                }
            }
        }
        
        // 4. Final Gap-fill (if we still need hours and have available slots on ANY day)
        if ((currentMinutes + addedMinutes) < requiredMinutes) {
            for (LocalDate date : workingDays) {
                if ((currentMinutes + addedMinutes) >= requiredMinutes) break;
                
                for (ParsedSlot slot : slots) {
                    if ((currentMinutes + addedMinutes) >= requiredMinutes) break;
                    
                    LocalDateTime proposedStart = date.atTime(slot.start);
                    Event tempEvent = new Event();
                    tempEvent.setCourseSeries(series);
                    tempEvent.setDurationMinutes(slot.duration);
                    
                    if (!hasCollision(tempEvent, proposedStart)) {
                        Room selectedRoom = null;
                        for (Room room : availableCandidateRooms) {
                            if ((room.getOperationalStatus() == de.campusplatform.campus_platform_service.model.OperationalStatus.AKTIV || 
                                 room.getOperationalStatus() == de.campusplatform.campus_platform_service.model.OperationalStatus.EINGESCHRAENKT) &&
                                hasCapacity(room, requiredSeats, EventType.LEHRVERANSTALTUNG) && 
                                isRoomAvailable(room, proposedStart, slot.duration, null)) {
                                selectedRoom = room;
                                break;
                            }
                        }
                        
                        if (selectedRoom != null) {
                            Event newEvent = new Event();
                            newEvent.setCourseSeries(series);
                            newEvent.setName(series.getModule().getName() + " (" + (series.getEvents().size() + 1) + ")");
                            newEvent.setEventType(EventType.LEHRVERANSTALTUNG);
                            newEvent.setStartTime(proposedStart);
                            newEvent.setDurationMinutes(slot.duration);
                            newEvent.setRooms(Set.of(selectedRoom));
                            
                            eventRepository.save(newEvent);
                            series.getEvents().add(newEvent);
                            addedMinutes += slot.duration;
                        }
                    }
                }
            }
        }
    }

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
                .filter(room -> room.getOperationalStatus() == de.campusplatform.campus_platform_service.model.OperationalStatus.AKTIV
                             || room.getOperationalStatus() == de.campusplatform.campus_platform_service.model.OperationalStatus.EINGESCHRAENKT)
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

        // 1b. Community Event Collision
        for (Long roomId : roomIds) {
            if (!communityEventRepository.findOverlappingEvents(roomId, event.getStartTime(), end, null).isEmpty()) {
                throw new AppException("eventManagement.communityEventCollision");
            }
        }

        // 2. Lecturer Collision
        if (event.getCourseSeries() != null && event.getCourseSeries().getAssignedLecturer() != null) {
            Long lecturerId = event.getCourseSeries().getAssignedLecturer().getId();

            if (lecturerAbsenceService.hasAbsenceConflict(lecturerId, event.getStartTime(), end)) {
                throw new de.campusplatform.campus_platform_service.exception.LecturerAbsenceConflictException(
                        "Terminkonflikt: Der Dozent hat in diesem Zeitraum eine Abwesenheit eingetragen.",
                        java.util.List.of());
            }

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

    public List<RoomScheduleEventResponse> getRoomSchedule(LocalDateTime start, LocalDateTime end) {
        List<Event> events = eventRepository.findAllEventsInRange(start, end);
        List<RoomScheduleEventResponse> result = new ArrayList<>();
        for (Event event : events) {
            for (Room room : event.getRooms()) {
                result.add(new RoomScheduleEventResponse(
                    event.getId(),
                    event.getName(),
                    event.getEventType().name(),
                    room.getId(),
                    room.getName(),
                    event.getStartTime() != null ? event.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null,
                    event.getDurationMinutes(),
                    event.getCourseSeries() != null ? event.getCourseSeries().getId() : null,
                    event.getCourseSeries() != null && event.getCourseSeries().getModule() != null
                        ? event.getCourseSeries().getModule().getName() : null
                ));
            }
        }
        return result;
    }

    public List<RoomUtilizationResponse> getRoomUtilizations(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        // Werktage zählen
        long workingDays = startDate.datesUntil(endDate.plusDays(1))
            .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY
                      && d.getDayOfWeek() != DayOfWeek.SUNDAY)
            .count();
        int totalAvailableMinutes = (int) workingDays * 600;

        List<Event> events = eventRepository.findAllEventsInRange(start, end);
        List<Room> allRooms = roomRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        return allRooms.stream().map(room -> {
            List<Event> roomEvents = events.stream()
                .filter(e -> e.getRooms().stream().anyMatch(r -> r.getId().equals(room.getId())))
                .collect(Collectors.toList());

            int bookedMinutes = roomEvents.stream()
                .mapToInt(Event::getDurationMinutes).sum();

            long plannedCount = roomEvents.stream()
                .filter(e -> e.getStartTime() != null && e.getStartTime().isAfter(now)).count();
            long pastCount = roomEvents.stream()
                .filter(e -> e.getStartTime() != null && !e.getStartTime().isAfter(now)).count();

            double utilization = totalAvailableMinutes > 0
                ? Math.min(100.0, (double) bookedMinutes / totalAvailableMinutes * 100)
                : 0.0;

            return new RoomUtilizationResponse(
                room.getId(), room.getName(), room.getSeats(), room.getExamSeats(),
                Math.round(utilization * 10.0) / 10.0,
                bookedMinutes, totalAvailableMinutes, plannedCount, pastCount
            );
        }).collect(Collectors.toList());
    }
}
