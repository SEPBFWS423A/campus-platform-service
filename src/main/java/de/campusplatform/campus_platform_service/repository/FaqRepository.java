package de.campusplatform.campus_platform_service.repository;

import de.campusplatform.campus_platform_service.model.Faq;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    @EntityGraph(attributePaths = "translations")
    List<Faq> findByPublishedTrueOrderBySortOrderAscIdAsc();

    @EntityGraph(attributePaths = "translations")
    List<Faq> findAllByOrderBySortOrderAscIdAsc();
}
