package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.FaqResponse;
import de.campusplatform.campus_platform_service.dto.FaqUpsertRequest;
import de.campusplatform.campus_platform_service.model.Faq;
import de.campusplatform.campus_platform_service.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    public List<FaqResponse> getVisibleFaqs() {
        return faqRepository.findByPublishedTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FaqResponse> getAllFaqs() {
        return faqRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public FaqResponse create(FaqUpsertRequest request) {
        Faq faq = Faq.builder()
                .question(request.question())
                .answer(request.answer())
                .category(request.category())
                .sortOrder(request.sortOrder())
                .published(request.published())
                .build();

        return toResponse(faqRepository.save(faq));
    }

    public FaqResponse update(Long id, FaqUpsertRequest request) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.faq_not_found"));

        faq.setQuestion(request.question());
        faq.setAnswer(request.answer());
        faq.setCategory(request.category());
        faq.setSortOrder(request.sortOrder());
        faq.setPublished(request.published());

        return toResponse(faqRepository.save(faq));
    }

    public void delete(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "error.faq_not_found");
        }

        faqRepository.deleteById(id);
    }

    private FaqResponse toResponse(Faq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getCategory(),
                faq.getSortOrder(),
                faq.getPublished()
        );
    }
}