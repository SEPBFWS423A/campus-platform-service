package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.StudentProfile;
import de.campusplatform.campus_platform_service.model.StudyGroup;
import de.campusplatform.campus_platform_service.model.StudyGroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyGroupMembershipRepository extends JpaRepository<StudyGroupMembership, Long> {
    List<StudyGroupMembership> findByStudentUserId(Long studentUserId);
    List<StudyGroupMembership> findByStudyGroupId(Long studyGroupId);
    java.util.Optional<StudyGroupMembership> findByStudentAndStudyGroup(StudentProfile student, StudyGroup studyGroup);
    boolean existsByStudentAndStudyGroup(StudentProfile student, StudyGroup studyGroup);
}
