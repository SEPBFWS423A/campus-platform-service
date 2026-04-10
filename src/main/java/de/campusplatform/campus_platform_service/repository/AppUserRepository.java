package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findByEmailIgnoreCase(String email);

    long countByRoleIn(List<Role> roles);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT u FROM CourseSeries cs " +
            "JOIN cs.studyGroups sg " +
            "JOIN sg.memberships m " +
            "JOIN m.student sp " +
            "JOIN sp.appUser u " +
            "WHERE cs.id = :seriesId")
    List<AppUser> findStudentsByCourseSeriesId(@org.springframework.data.repository.query.Param("seriesId") Long seriesId);
}
