package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.model.*;
import de.campusplatform.campus_platform_service.repository.*;
import de.campusplatform.campus_platform_service.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import de.campusplatform.campus_platform_service.enums.HolidayType;

import java.security.Principal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final RoomRepository roomRepository;        
    private final StudyGroupService studyGroupService;
    private final ModuleService moduleService;
    private final CourseOfStudyRepository courseOfStudyRepository;
    private final SpecializationRepository specializationRepository;
    private final InstitutionRepository institutionRepository;
    private final ExamTypeRepository examTypeRepository;
    private final FaqService faqService;  
    private final CourseSeriesService courseSeriesService;
    private final EventService eventService;
    private final LecturerAbsenceService lecturerAbsenceService;
    private final RoomBlockoutService roomBlockoutService;
    private final RoomStatusHistoryRepository statusHistoryRepository;
    private final JobPostingService jobPostingService;
    private final ApplicationService applicationService;
    private final HolidayRepository holidayRepository;

    @PostMapping("/invitations")
    public ResponseEntity<Void> inviteUser(@RequestBody InvitationRequest request) {
        authService.inviteUser(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/invitations/bulk")
    public ResponseEntity<Void> bulkInvite(@RequestBody BulkInvitationRequest request) {
        request.invitations().forEach(authService::inviteUser);
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

    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    @GetMapping("/rooms/available")
    public ResponseEntity<List<Room>> getAvailableRooms(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false) Long excludeEventId,
            @RequestParam(required = false) Long seriesId,
            @RequestParam(required = false) String eventType) {
        return ResponseEntity.ok(eventService.getAvailableRooms(startTime, durationMinutes, excludeEventId, seriesId, eventType));
    }

    @GetMapping("/rooms/schedule")
    public ResponseEntity<List<RoomScheduleEventResponse>> getRoomSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(eventService.getRoomSchedule(start, end));
    }

    @GetMapping("/rooms/utilization")
    public ResponseEntity<List<RoomUtilizationResponse>> getRoomUtilizations(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(eventService.getRoomUtilizations(startDate, endDate));
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
            room.setBuilding(updated.getBuilding());
            room.setFloor(updated.getFloor());
            room.setRoomType(updated.getRoomType());
            room.setOperationalStatus(updated.getOperationalStatus());
            room.setFeatures(updated.getFeatures());
            room.setBarrierefreiheit(updated.getBarrierefreiheit());
            room.setDescription(updated.getDescription());
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

    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<Room> updateRoomStatus(@PathVariable Long id, @RequestBody StatusRequest request, Principal principal) {
        Room room = roomRepository.findById(id).orElseThrow();
        OperationalStatus oldStatus = room.getOperationalStatus();
        OperationalStatus newStatus = OperationalStatus.valueOf(request.status());
        
        room.setOperationalStatus(newStatus);
        Room saved = roomRepository.save(room);

        statusHistoryRepository.save(RoomStatusHistory.builder()
                .room(saved)
                .previousStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(principal.getName())
                .changedAt(LocalDateTime.now())
                .reason(request.reason())
                .build());

        return ResponseEntity.ok(saved);
    }


    // Room Blockouts
    @PostMapping("/rooms/blockouts")
    public ResponseEntity<RoomBlockoutResponse> createBlockout(@RequestBody RoomBlockoutRequest req, Principal principal) {
        return ResponseEntity.ok(roomBlockoutService.createBlockout(req, principal.getName()));
    }

    @GetMapping("/rooms/blockouts")
    public ResponseEntity<List<RoomBlockoutResponse>> getBlockouts(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(roomBlockoutService.getBlockouts(roomId, active));
    }

    @PatchMapping("/rooms/blockouts/{id}/resolve")
    public ResponseEntity<RoomBlockoutResponse> resolveBlockout(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(roomBlockoutService.resolveBlockout(id, principal.getName()));
    }

    @DeleteMapping("/rooms/blockouts/{id}")
    public ResponseEntity<Void> deleteBlockout(@PathVariable Long id) {
        roomBlockoutService.deleteBlockout(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms/{id}/status-history")
    public ResponseEntity<List<RoomStatusHistory>> getStatusHistory(@PathVariable Long id) {
        return ResponseEntity.ok(statusHistoryRepository.findByRoomIdOrderByChangedAtDesc(id));
    }


    @GetMapping("/rooms/{id}/blockouts/conflicts")
    public ResponseEntity<BlockoutConflictResult> checkConflicts(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(roomBlockoutService.checkConflicts(id, start, end));
    }



    // Study Groups
    @GetMapping("/groups")
    public ResponseEntity<List<AdminGroupResponse>> getAllGroups() {
        return ResponseEntity.ok(studyGroupService.getAllGroupsForAdmin());
    }

    @PostMapping("/groups")
    public ResponseEntity<AdminGroupResponse> createGroup(@RequestBody StudyGroupRequest request) {
        return ResponseEntity.ok(studyGroupService.createGroup(request));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<AdminGroupResponse> updateGroup(@PathVariable Long id, @RequestBody StudyGroupRequest request) {
        return ResponseEntity.ok(studyGroupService.updateGroup(id, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        studyGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Void> addGroupMember(@PathVariable Long groupId, @PathVariable Long userId) {
        studyGroupService.addStudentToGroup(userId, groupId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeGroupMember(@PathVariable Long groupId, @PathVariable Long userId) {
        studyGroupService.removeStudentFromGroup(userId, groupId);
        return ResponseEntity.ok().build();
    }

    // Course of Study
    @GetMapping("/courses")
    public ResponseEntity<List<AdminCourseResponse>> getAllCourses() {
        return ResponseEntity.ok(courseOfStudyRepository.findAll().stream()
                .map(c -> new AdminCourseResponse(c.getId(), c.getName(), c.getDegreeType()))
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/courses")
    public ResponseEntity<AdminCourseResponse> createCourse(@RequestBody CourseRequest request) {
        CourseOfStudy cos = CourseOfStudy.builder()
                .name(request.name())
                .degreeType(request.degreeType())
                .build();
        CourseOfStudy saved = courseOfStudyRepository.save(cos);
        return ResponseEntity.ok(new AdminCourseResponse(saved.getId(), saved.getName(), saved.getDegreeType()));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<AdminCourseResponse> updateCourse(@PathVariable Long id, @RequestBody CourseRequest request) {
        CourseOfStudy current = courseOfStudyRepository.findById(id).orElseThrow();
        current.setName(request.name());
        current.setDegreeType(request.degreeType());
        CourseOfStudy saved = courseOfStudyRepository.save(current);
        return ResponseEntity.ok(new AdminCourseResponse(saved.getId(), saved.getName(), saved.getDegreeType()));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        try {
            courseOfStudyRepository.deleteById(id);
            courseOfStudyRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new de.campusplatform.campus_platform_service.exception.AppException("error.course.referenced");
        }
        return ResponseEntity.ok().build();
    }

    // Specializations
    @GetMapping("/specializations")
    public ResponseEntity<List<AdminSpecializationResponse>> getAllSpecializations() {
        return ResponseEntity.ok(specializationRepository.findAll().stream()
                .map(s -> new AdminSpecializationResponse(s.getId(), s.getName(), s.getCourseOfStudy().getId()))
                .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/specializations")
    public ResponseEntity<AdminSpecializationResponse> createSpecialization(@RequestBody SpecializationRequest request) {
        CourseOfStudy cos = courseOfStudyRepository.findById(request.courseId())
                .orElseThrow();
        Specialization s = Specialization.builder()
                .name(request.name())
                .courseOfStudy(cos)
                .build();
        Specialization saved = specializationRepository.save(s);
        return ResponseEntity.ok(new AdminSpecializationResponse(saved.getId(), saved.getName(), saved.getCourseOfStudy().getId()));
    }

    @PutMapping("/specializations/{id}")
    public ResponseEntity<AdminSpecializationResponse> updateSpecialization(@PathVariable Long id, @RequestBody SpecializationRequest request) {
        Specialization current = specializationRepository.findById(id).orElseThrow();
        current.setName(request.name());
        if (request.courseId() != null) {
            CourseOfStudy cos = courseOfStudyRepository.findById(request.courseId()).orElseThrow();
            current.setCourseOfStudy(cos);
        }
        Specialization saved = specializationRepository.save(current);
        return ResponseEntity.ok(new AdminSpecializationResponse(saved.getId(), saved.getName(), saved.getCourseOfStudy().getId()));
    }

    @DeleteMapping("/specializations/{id}")
    public ResponseEntity<Void> deleteSpecialization(@PathVariable Long id) {
        try {
            specializationRepository.deleteById(id);
            specializationRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new de.campusplatform.campus_platform_service.exception.AppException("error.specialization.referenced");
        }
        return ResponseEntity.ok().build();
    }

    // Institution Information
    @PutMapping("/institution")
    public ResponseEntity<InstitutionInfo> updateInstitutionInfo(@RequestBody InstitutionInfo info) {
        InstitutionInfo current = institutionRepository.getFirst().orElse(new InstitutionInfo());
        current.setUniversityName(info.getUniversityName());
        current.setCity(info.getCity());
        current.setSekretariatEmail(info.getSekretariatEmail());
        current.setSekretariatPhone(info.getSekretariatPhone());
        current.setSekretariatOpeningTimes(info.getSekretariatOpeningTimes());
        current.setWebsiteEmail(info.getWebsiteEmail());
        current.setBibliothekUrl(info.getBibliothekUrl());
        current.setMensaUrl(info.getMensaUrl());
        current.setImpressum(info.getImpressum());
        current.setInvitationEmailSubjectDe(info.getInvitationEmailSubjectDe());
        current.setInvitationEmailBodyDe(info.getInvitationEmailBodyDe());
        current.setInvitationEmailSubjectEn(info.getInvitationEmailSubjectEn());
        current.setInvitationEmailBodyEn(info.getInvitationEmailBodyEn());
        current.setPasswordResetEmailSubjectDe(info.getPasswordResetEmailSubjectDe());
        current.setPasswordResetEmailBodyDe(info.getPasswordResetEmailBodyDe());
        current.setPasswordResetEmailSubjectEn(info.getPasswordResetEmailSubjectEn());
        current.setPasswordResetEmailBodyEn(info.getPasswordResetEmailBodyEn());
        return ResponseEntity.ok(institutionRepository.save(current));
    }

    // Exam Types
    @GetMapping("/exam-types")
    public ResponseEntity<List<ExamType>> getAllExamTypes() {
        return ResponseEntity.ok(examTypeRepository.findAll());
    }

    @PostMapping("/exam-types")
    public ResponseEntity<ExamType> createExamType(@RequestBody ExamType examType) {
        return ResponseEntity.ok(examTypeRepository.save(examType));
    }

    @PutMapping("/exam-types/{id}")
    public ResponseEntity<ExamType> updateExamType(@PathVariable Long id, @RequestBody ExamType examType) {
        ExamType current = examTypeRepository.findById(id).orElseThrow();
        current.setType(examType.getType());
        current.setSubmission(examType.isSubmission());
        current.setNameDe(examType.getNameDe());
        current.setNameEn(examType.getNameEn());
        current.setShortDe(examType.getShortDe());
        current.setShortEn(examType.getShortEn());
        return ResponseEntity.ok(examTypeRepository.save(current));
    }

    @DeleteMapping("/exam-types/{id}")
    public ResponseEntity<Void> deleteExamType(@PathVariable Long id) {
        try {
            examTypeRepository.deleteById(id);
            examTypeRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new de.campusplatform.campus_platform_service.exception.AppException("error.examType.referenced");
        }
        return ResponseEntity.ok().build();
    }

    // Modules
    @GetMapping("/modules")
    public ResponseEntity<List<AdminModuleResponse>> getAllModules() {
        return ResponseEntity.ok(moduleService.getAllModulesForAdmin());
    }

    @PostMapping("/modules")
    public ResponseEntity<Void> createModule(@RequestBody ModuleRequest request) {
        moduleService.createModule(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/modules/{id}")
    public ResponseEntity<AdminModuleResponse> updateModule(@PathVariable Long id, @RequestBody ModuleRequest request) {
        return ResponseEntity.ok(moduleService.updateModule(id, request));
    }

    @DeleteMapping("/modules/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable Long id) {
        moduleService.deleteModule(id);
        return ResponseEntity.ok().build();
    }

    // FAQ
    @GetMapping("/faqs")
    public List<FaqAdminResponse> getAllFaqs() {
        return faqService.getAllFaqsForAdmin();
    }

    @PostMapping("/faqs")
    public FaqAdminResponse createFaq(@Valid @RequestBody FaqUpsertRequest request) {
        return faqService.create(request);
    }

    @PutMapping("/faqs/{id}")
    public FaqAdminResponse updateFaq(@PathVariable Long id,
                                      @Valid @RequestBody FaqUpsertRequest request) {
        return faqService.update(id, request);
    }

    @DeleteMapping("/faqs/{id}")
    public void deleteFaq(@PathVariable Long id) {
        faqService.delete(id);
    }

    // Course Series
    @GetMapping("/course-series")
    public ResponseEntity<List<AdminCourseSeriesResponse>> getAllCourseSeries() {
        return ResponseEntity.ok(courseSeriesService.getAllCourseSeries());
    }

    @GetMapping("/course-series/{id}")
    public ResponseEntity<AdminCourseSeriesResponse> getCourseSeriesById(@PathVariable Long id) {
        return ResponseEntity.ok(courseSeriesService.getCourseSeriesById(id));
    }

    @PostMapping("/course-series")
    public ResponseEntity<AdminCourseSeriesResponse> createCourseSeries(@Valid @RequestBody CourseSeriesRequest request) {
        return ResponseEntity.ok(courseSeriesService.createCourseSeries(request));
    }

    @PutMapping("/course-series/{id}")
    public ResponseEntity<AdminCourseSeriesResponse> updateCourseSeries(@PathVariable Long id, @Valid @RequestBody CourseSeriesRequest request) {
        return ResponseEntity.ok(courseSeriesService.updateCourseSeries(id, request));
    }

    @DeleteMapping("/course-series/{id}")
    public ResponseEntity<Void> deleteCourseSeries(@PathVariable Long id) {
        courseSeriesService.deleteCourseSeries(id);
        return ResponseEntity.ok().build();
    }

    // Events
    @GetMapping("/course-series/{seriesId}/events")
    public ResponseEntity<List<AdminEventResponse>> getEventsForSeries(@PathVariable Long seriesId) {
        return ResponseEntity.ok(eventService.getEventsForSeries(seriesId));
    }

    @PostMapping("/course-series/{seriesId}/events")
    public ResponseEntity<AdminEventResponse> createEvent(@PathVariable Long seriesId, @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.createEvent(seriesId, request));
    }

    @PostMapping("/course-series/{seriesId}/fast-add-event")
    public ResponseEntity<AdminEventResponse> fastAddEvent(@PathVariable Long seriesId) {
        return ResponseEntity.ok(eventService.fastAddEvent(seriesId));
    }

    @PutMapping("/events/{eventId}")
    public ResponseEntity<AdminEventResponse> updateEvent(@PathVariable Long eventId, @RequestBody EventRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/course-series/{seriesId}/auto-schedule")
    public ResponseEntity<Void> autoSchedule(@PathVariable Long seriesId, @RequestBody AutoScheduleRequest request) {
        eventService.autoSchedule(seriesId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/absences")
    public ResponseEntity<List<LecturerAbsenceResponse>> getAllAbsences() {
        return ResponseEntity.ok(lecturerAbsenceService.getAllAbsences());
    }

    @DeleteMapping("/absences/{id}")
    public ResponseEntity<Void> deleteAbsenceAsAdmin(@PathVariable Long id) {
        lecturerAbsenceService.deleteAbsence(id, null, true);
        return ResponseEntity.ok().build();
    }

    /** Sub-Issue #292: Abwesenheit genehmigen */
    @PatchMapping("/absences/{id}/approve")
    public ResponseEntity<LecturerAbsenceResponse> approveAbsence(
            @PathVariable Long id, java.security.Principal principal) {
        return ResponseEntity.ok(lecturerAbsenceService.approveAbsence(id, principal.getName()));
    }

    /** Sub-Issue #292: Abwesenheit ablehnen */
    @PatchMapping("/absences/{id}/reject")
    public ResponseEntity<LecturerAbsenceResponse> rejectAbsence(
            @PathVariable Long id,
            @RequestBody de.campusplatform.campus_platform_service.dto.RejectAbsenceRequest req,
            java.security.Principal principal) {
        return ResponseEntity.ok(lecturerAbsenceService.rejectAbsence(id, req.reason(), principal.getName()));
    }

    /** Sub-Issue #297: Audit-Trail abrufen */
    @GetMapping("/absences/{id}/history")
    public ResponseEntity<List<de.campusplatform.campus_platform_service.model.AbsenceAuditLog>> getAbsenceHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(lecturerAbsenceService.getHistory(id));
    }

    // =========================================================================
    // Stellenausschreibungen (Issue #324)
    // =========================================================================

    @GetMapping("/job-postings")
    public ResponseEntity<List<JobPostingResponse>> getAllJobPostings(
            @RequestParam(required = false) de.campusplatform.campus_platform_service.enums.JobStatus status) {
        if (status != null) {
            return ResponseEntity.ok(jobPostingService.getByStatus(status));
        }
        return ResponseEntity.ok(jobPostingService.getAll());
    }

    @GetMapping("/job-postings/{id}")
    public ResponseEntity<JobPostingResponse> getJobPosting(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.getById(id));
    }

    @PostMapping("/job-postings")
    public ResponseEntity<JobPostingResponse> createJobPosting(
            @Valid @RequestBody JobPostingRequest req,
            Principal principal) {
        return ResponseEntity.status(201).body(jobPostingService.create(req, principal.getName()));
    }

    @PutMapping("/job-postings/{id}")
    public ResponseEntity<JobPostingResponse> updateJobPosting(
            @PathVariable Long id,
            @Valid @RequestBody JobPostingRequest req) {
        return ResponseEntity.ok(jobPostingService.update(id, req));
    }

    @PatchMapping("/job-postings/{id}/status")
    public ResponseEntity<JobPostingResponse> setJobPostingStatus(
            @PathVariable Long id,
            @RequestParam de.campusplatform.campus_platform_service.enums.JobStatus status) {
        return ResponseEntity.ok(jobPostingService.setStatus(id, status));
    }

    @DeleteMapping("/job-postings/{id}")
    public ResponseEntity<Void> deleteJobPosting(@PathVariable Long id) {
        jobPostingService.delete(id);
        return ResponseEntity.noContent().build();
    }
    // Applications
    @GetMapping("/applications")
    public ResponseEntity<List<AdminApplicationResponse>> getAllApplications() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<AdminApplicationResponse> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(applicationService.updateStatus(id, status));
    }
    // Holidays
@GetMapping("/holidays")
public ResponseEntity<List<HolidayResponse>> getAllHolidays() {
    return ResponseEntity.ok(holidayRepository.findAllByOrderByStartDateAsc()
        .stream().map(HolidayResponse::from).toList());
}

@PostMapping("/holidays")
public ResponseEntity<HolidayResponse> createHoliday(@RequestBody HolidayRequest request) {
    Holiday holiday = new Holiday();
    holiday.setName(request.name());
    holiday.setStartDate(request.startDate());
    holiday.setEndDate(request.endDate());
    holiday.setType(de.campusplatform.campus_platform_service.enums.HolidayType.valueOf(request.type().toUpperCase()));
    return ResponseEntity.ok(HolidayResponse.from(holidayRepository.save(holiday)));
}

@DeleteMapping("/holidays/{id}")
public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
    holidayRepository.deleteById(id);
    return ResponseEntity.noContent().build();
}
}
