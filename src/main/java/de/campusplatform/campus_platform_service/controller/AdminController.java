package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.model.ExamType;
import de.campusplatform.campus_platform_service.repository.ExamTypeRepository;
import de.campusplatform.campus_platform_service.model.Room;
import de.campusplatform.campus_platform_service.repository.RoomRepository;
import de.campusplatform.campus_platform_service.service.AuthService;
import de.campusplatform.campus_platform_service.service.ModuleService;
import de.campusplatform.campus_platform_service.service.StudyGroupService;
import de.campusplatform.campus_platform_service.repository.InstitutionRepository;
import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import de.campusplatform.campus_platform_service.repository.CourseOfStudyRepository;
import de.campusplatform.campus_platform_service.repository.SpecializationRepository;
import de.campusplatform.campus_platform_service.model.CourseOfStudy;
import de.campusplatform.campus_platform_service.model.Specialization;
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
    private final StudyGroupService studyGroupService;
    private final ModuleService moduleService;
    private final CourseOfStudyRepository courseOfStudyRepository;
    private final SpecializationRepository specializationRepository;
    private final InstitutionRepository institutionRepository;
    private final ExamTypeRepository examTypeRepository;

    public AdminController(AuthService authService, 
                           RoomRepository roomRepository,
                           StudyGroupService studyGroupService, 
                           ModuleService moduleService,
                           CourseOfStudyRepository courseOfStudyRepository,
                           SpecializationRepository specializationRepository,
                           InstitutionRepository institutionRepository,
                           ExamTypeRepository examTypeRepository) {
        this.authService = authService;
        this.roomRepository = roomRepository;
        this.studyGroupService = studyGroupService;
        this.moduleService = moduleService;
        this.courseOfStudyRepository = courseOfStudyRepository;
        this.specializationRepository = specializationRepository;
        this.institutionRepository = institutionRepository;
        this.examTypeRepository = examTypeRepository;
    }

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

    // Study Groups
    @GetMapping("/groups")
    public ResponseEntity<List<AdminGroupResponse>> getAllGroups() {
        return ResponseEntity.ok(studyGroupService.getAllGroupsForAdmin());
    }

    @PostMapping("/groups")
    public ResponseEntity<Void> createGroup(@RequestBody StudyGroupRequest request) {
        studyGroupService.createGroup(request);
        return ResponseEntity.ok().build();
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

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseOfStudyRepository.deleteById(id);
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

    @DeleteMapping("/specializations/{id}")
    public ResponseEntity<Void> deleteSpecialization(@PathVariable Long id) {
        specializationRepository.deleteById(id);
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
        current.setNameDe(examType.getNameDe());
        current.setNameEn(examType.getNameEn());
        current.setShortDe(examType.getShortDe());
        current.setShortEn(examType.getShortEn());
        return ResponseEntity.ok(examTypeRepository.save(current));
    }

    @DeleteMapping("/exam-types/{id}")
    public ResponseEntity<Void> deleteExamType(@PathVariable Long id) {
        examTypeRepository.deleteById(id);
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
}
