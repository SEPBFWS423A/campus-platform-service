package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.StudyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudyProgramRepository extends JpaRepository<StudyProgram, Long> {
    List<StudyProgram> findByActiveTrue();
}