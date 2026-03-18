package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
}
