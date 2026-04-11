package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByStudentNumber(String studentNumber);
    Optional<StudentProfile> findByAppUserId(Long userId);
    
    @org.springframework.data.jpa.repository.Query("SELECT MAX(s.studentNumber) FROM StudentProfile s")
    Optional<String> findMaxStudentNumber();
}
