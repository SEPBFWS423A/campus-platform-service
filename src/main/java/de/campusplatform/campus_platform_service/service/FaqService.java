package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.model.Faq;
import de.campusplatform.campus_platform_service.model.FaqTranslation;
import de.campusplatform.campus_platform_service.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FaqService {

    private final FaqRepository faqRepository;

    public List<FaqResponse> getVisibleFaqs(String lang) {
        return faqRepository.findByPublishedTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(faq -> toUserResponse(faq, lang))
                .toList();
    }

    public List<FaqAdminResponse> getAllFaqsForAdmin() {
        return faqRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public FaqAdminResponse create(FaqUpsertRequest request) {
        Faq faq = Faq.builder()
                .sortOrder(request.sortOrder())
                .published(request.published())
                .build();

        applyTranslations(faq, request.translations());

        return toAdminResponse(faqRepository.save(faq));
    }

    public FaqAdminResponse update(Long id, FaqUpsertRequest request) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.faq_not_found"));

        faq.setSortOrder(request.sortOrder());
        faq.setPublished(request.published());

        faq.getTranslations().clear();
        applyTranslations(faq, request.translations());

        return toAdminResponse(faqRepository.save(faq));
    }

    public void delete(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "error.faq_not_found");
        }
        faqRepository.deleteById(id);
    }

    private void applyTranslations(Faq faq, List<FaqTranslationRequest> translationRequests) {
        for (FaqTranslationRequest t : translationRequests) {
            faq.getTranslations().add(
                    FaqTranslation.builder()
                            .faq(faq)
                            .languageCode(normalizeLanguage(t.languageCode()))
                            .question(t.question())
                            .answer(t.answer())
                            .category(t.category())
                            .build()
            );
        }
    }

    private FaqResponse toUserResponse(Faq faq, String lang) {
        String normalizedLang = normalizeLanguage(lang);

        FaqTranslation translation = faq.getTranslations().stream()
                .filter(t -> t.getLanguageCode().equals(normalizedLang))
                .findFirst()
                .orElseGet(() -> faq.getTranslations().stream()
                        .filter(t -> t.getLanguageCode().equals("de"))
                        .findFirst()
                        .orElseGet(() -> faq.getTranslations().stream()
                                .min(Comparator.comparing(FaqTranslation::getLanguageCode))
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "error.faq_translation_missing"
                                ))
                        ));

        return new FaqResponse(
                faq.getId(),
                translation.getQuestion(),
                translation.getAnswer(),
                translation.getCategory(),
                faq.getSortOrder(),
                faq.getPublished()
        );
    }

    private FaqAdminResponse toAdminResponse(Faq faq) {
        List<FaqTranslationAdminResponse> translations = faq.getTranslations().stream()
                .sorted(Comparator.comparing(FaqTranslation::getLanguageCode))
                .map(t -> new FaqTranslationAdminResponse(
                        t.getId(),
                        t.getLanguageCode(),
                        t.getQuestion(),
                        t.getAnswer(),
                        t.getCategory()
                ))
                .toList();

        return new FaqAdminResponse(
                faq.getId(),
                faq.getSortOrder(),
                faq.getPublished(),
                translations
        );
    }

    private String normalizeLanguage(String lang) {
        if (lang == null || lang.isBlank()) {
            return "de";
        }
        return lang.toLowerCase(Locale.ROOT).trim();
    }
}