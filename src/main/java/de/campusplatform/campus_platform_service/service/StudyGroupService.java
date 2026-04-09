package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.AdminGroupResponse;
import de.campusplatform.campus_platform_service.dto.StudyGroupRequest;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.*;
import de.campusplatform.campus_platform_service.repository.SpecializationRepository;
import de.campusplatform.campus_platform_service.repository.StudentProfileRepository;
import de.campusplatform.campus_platform_service.repository.StudyGroupMembershipRepository;
import de.campusplatform.campus_platform_service.repository.StudyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyGroupService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMembershipRepository membershipRepository;
    private final SpecializationRepository specializationRepository;

    /**
     * Requirement 3: Business Logic & Validation Rules
     * Implementation for adding a student to a study group.
     * 
     * @param userId The ID of the student to add.
     * @param groupId The ID of the study group.
     * @return The created StudyGroupMembership.
     * @throws AppException if validation fails or records are not found.
     */
    @Transactional
    public StudyGroupMembership addStudentToGroup(Long userId, Long groupId) {
        // 1. Role Verification
        // Ensure the target user is a Student (i.e., they possess a valid StudentProfile).
        // If a StudentProfile exists for a userId, it confirms they are a student by design.
        StudentProfile student = studentProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException("USER_NOT_STUDENT"));

        // 2. Fetch Study Group
        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException("STUDY_GROUP_NOT_FOUND"));

        // 3. Specialization Matching
        // Condition: IF StudyGroup.specialization_id == StudentProfile.specialization_id, proceed.
        if (!Objects.equals(student.getSpecialization().getId(), studyGroup.getSpecialization().getId())) {
            // Exception: ELSE, throw a validation error
            throw new AppException("STUDENT_SPECIALIZATION_MISMATCH");
        }

        // Prevent duplicate membership
        if (membershipRepository.existsByStudentAndStudyGroup(student, studyGroup)) {
            throw new AppException("ALREADY_MEMBER");
        }

        // 4. Create and persist StudyGroupMembership
        StudyGroupMembership membership = StudyGroupMembership.builder()
                .student(student)
                .studyGroup(studyGroup)
                .build();

        return membershipRepository.save(membership);
    }

    @Transactional
    public void removeStudentFromGroup(Long userId, Long groupId) {
        StudentProfile student = studentProfileRepository.findById(userId)
                .orElseThrow(() -> new AppException("USER_NOT_STUDENT"));

        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException("STUDY_GROUP_NOT_FOUND"));

        StudyGroupMembership membership = membershipRepository.findByStudentAndStudyGroup(student, studyGroup)
                .orElseThrow(() -> new AppException("NOT_A_MEMBER"));

        membershipRepository.delete(membership);
    }

    public List<AdminGroupResponse> getAllGroupsForAdmin() {
        return studyGroupRepository.findAll().stream()
                .map(group -> {
                    List<AdminGroupResponse.GroupMemberDTO> members = group.getMemberships().stream()
                            .map(m -> new AdminGroupResponse.GroupMemberDTO(
                                    m.getStudent().getAppUser().getId(),
                                    m.getStudent().getStudentNumber(),
                                    m.getStudent().getAppUser().getTitle(),
                                    m.getStudent().getAppUser().getFirstName(),
                                    m.getStudent().getAppUser().getLastName()
                            ))
                            .collect(Collectors.toList());

                    return new AdminGroupResponse(
                            group.getId(),
                            group.getName(),
                            group.getSpecialization().getCourseOfStudy().getId(),
                            group.getSpecialization().getCourseOfStudy().getName(),
                            group.getSpecialization().getId(),
                            group.getSpecialization().getName(),
                            members.size(),
                            group.getStartYear(),
                            group.getStartQuartal(),
                            members
                    );
                })
                .collect(Collectors.toList());
    }

    private AdminGroupResponse mapToResponse(StudyGroup group) {
        List<AdminGroupResponse.GroupMemberDTO> members = (group.getMemberships() != null ? group.getMemberships() : new java.util.HashSet<StudyGroupMembership>())
                .stream()
                .map(m -> new AdminGroupResponse.GroupMemberDTO(
                        m.getStudent().getAppUser().getId(),
                        m.getStudent().getStudentNumber(),
                        m.getStudent().getAppUser().getTitle(),
                        m.getStudent().getAppUser().getFirstName(),
                        m.getStudent().getAppUser().getLastName()
                ))
                .collect(Collectors.toList());

        return new AdminGroupResponse(
                group.getId(),
                group.getName(),
                group.getSpecialization().getCourseOfStudy().getId(),
                group.getSpecialization().getCourseOfStudy().getName(),
                group.getSpecialization().getId(),
                group.getSpecialization().getName(),
                members.size(),
                group.getStartYear(),
                group.getStartQuartal(),
                members
        );
    }

    @Transactional
    public AdminGroupResponse createGroup(StudyGroupRequest request) {
        if (studyGroupRepository.findByName(request.name()).isPresent()) {
            throw new AppException("error.group.alreadyExists");
        }

        Specialization specialization = specializationRepository.findById(request.specializationId())
                .orElseThrow(() -> new AppException("error.specialization.notFound"));

        StudyGroup group = StudyGroup.builder()
                .name(request.name())
                .specialization(specialization)
                .startYear(request.startYear())
                .startQuartal(request.startQuartal())
                .build();

        StudyGroup saved = studyGroupRepository.save(group);
        return mapToResponse(saved);
    }

    @Transactional
    public AdminGroupResponse updateGroup(Long id, StudyGroupRequest request) {
        StudyGroup group = studyGroupRepository.findById(id)
                .orElseThrow(() -> new AppException("error.group.notFound"));

        if (!group.getName().equals(request.name()) && studyGroupRepository.findByName(request.name()).isPresent()) {
            throw new AppException("error.group.alreadyExists");
        }

        Specialization specialization = specializationRepository.findById(request.specializationId())
                .orElseThrow(() -> new AppException("error.specialization.notFound"));

        group.setName(request.name());
        group.setSpecialization(specialization);
        group.setStartYear(request.startYear());
        group.setStartQuartal(request.startQuartal());
        StudyGroup saved = studyGroupRepository.save(group);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteGroup(Long id) {
        if (!studyGroupRepository.existsById(id)) {
            throw new AppException("error.group.notFound");
        }
        try {
            studyGroupRepository.deleteById(id);
            studyGroupRepository.flush(); // Force flush to catch constraint violations early
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new AppException("error.group.referenced");
        }
    }
}
