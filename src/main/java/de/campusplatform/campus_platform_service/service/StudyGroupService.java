package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.StudentProfile;
import de.campusplatform.campus_platform_service.model.StudyGroup;
import de.campusplatform.campus_platform_service.model.StudyGroupMembership;
import de.campusplatform.campus_platform_service.repository.StudentProfileRepository;
import de.campusplatform.campus_platform_service.repository.StudyGroupMembershipRepository;
import de.campusplatform.campus_platform_service.repository.StudyGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudyGroupService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMembershipRepository membershipRepository;

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

        // 3. Focus Matching
        // Condition: IF StudyGroup.focus_id == StudentProfile.focus_id, proceed.
        if (!Objects.equals(student.getFocus().getId(), studyGroup.getFocus().getId())) {
            // Exception: ELSE, throw a validation error
            throw new AppException("STUDENT_FOCUS_MISMATCH");
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
}
