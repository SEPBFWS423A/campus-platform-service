package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.InstitutionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InstitutionRepository extends JpaRepository<InstitutionInfo, Long> {
    default Optional<InstitutionInfo> getFirst() {
        return findAll().stream().findFirst();
    }
}
