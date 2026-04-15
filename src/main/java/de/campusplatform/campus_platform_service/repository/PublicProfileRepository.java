package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.PublicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicProfileRepository extends JpaRepository<PublicProfile, Long> {
    Optional<PublicProfile> findByAppUserEmail(String email);

    List<PublicProfile> findAllByVisibilityTrueAndAppUserEmailNot(String currentUserEmail);
}
