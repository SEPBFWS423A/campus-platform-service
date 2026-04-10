package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.*;
import de.campusplatform.campus_platform_service.model.Faq;
import de.campusplatform.campus_platform_service.model.FaqTranslation;
import de.campusplatform.campus_platform_service.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqService {

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<FaqResponse> getVisibleFaqs(String lang) {
        return faqRepository.findByPublishedTrueOrderBySortOrderAscIdAsc()
                .stream()
                .map(faq -> toUserResponse(faq, lang))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FaqAdminResponse> getAllFaqsForAdmin() {
        return faqRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    public FaqAdminResponse create(FaqUpsertRequest request) {
        validateTranslations(request.translations());

        Faq faq = Faq.builder()
                .sortOrder(request.sortOrder())
                .published(request.published())
                .build();

        addTranslations(faq, request.translations());

        return toAdminResponse(faqRepository.save(faq));
    }

    public FaqAdminResponse update(Long id, FaqUpsertRequest request) {
        validateTranslations(request.translations());

        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "error.faq_not_found"));

        faq.setSortOrder(request.sortOrder());
        faq.setPublished(request.published());

        syncTranslations(faq, request.translations());

        return toAdminResponse(faqRepository.save(faq));
    }

    public void delete(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "error.faq_not_found");
        }
        faqRepository.deleteById(id);
    }

    private void addTranslations(Faq faq, List<FaqTranslationRequest> translationRequests) {
        for (FaqTranslationRequest t : translationRequests) {
            faq.getTranslations().add(
                    FaqTranslation.builder()
                            .faq(faq)
                            .languageCode(normalizeLanguage(t.languageCode()))
                            .question(t.question().trim())
                            .answer(t.answer().trim())
                            .category(t.category().trim())
                            .build()
            );
        }
    }

    private void syncTranslations(Faq faq, List<FaqTranslationRequest> translationRequests) {
        Map<String, FaqTranslationRequest> requestedByLanguage = translationRequests.stream()
                .collect(Collectors.toMap(
                        t -> normalizeLanguage(t.languageCode()),
                        Function.identity(),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        Map<String, FaqTranslation> existingByLanguage = faq.getTranslations().stream()
                .collect(Collectors.toMap(
                        t -> normalizeLanguage(t.getLanguageCode()),
                        Function.identity()
                ));

        Set<String> requestedLanguages = requestedByLanguage.keySet();

        faq.getTranslations().removeIf(existing -> !requestedLanguages.contains(normalizeLanguage(existing.getLanguageCode())));

        for (Map.Entry<String, FaqTranslationRequest> entry : requestedByLanguage.entrySet()) {
            String language = entry.getKey();
            FaqTranslationRequest request = entry.getValue();

            FaqTranslation existing = existingByLanguage.get(language);

            if (existing != null) {
                existing.setLanguageCode(language);
                existing.setQuestion(request.question().trim());
                existing.setAnswer(request.answer().trim());
                existing.setCategory(request.category().trim());
            } else {
                faq.getTranslations().add(
                        FaqTranslation.builder()
                                .faq(faq)
                                .languageCode(language)
                                .question(request.question().trim())
                                .answer(request.answer().trim())
                                .category(request.category().trim())
                                .build()
                );
            }
        }
    }

    private void validateTranslations(List<FaqTranslationRequest> translationRequests) {
        if (translationRequests == null || translationRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.faq_translation_missing");
        }

        Set<String> languages = new HashSet<>();

        for (FaqTranslationRequest t : translationRequests) {
            String language = normalizeLanguage(t.languageCode());

            if (!languages.add(language)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.faq_duplicate_language");
            }

            if (t.question() == null || t.question().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.faq_question_required");
            }

            if (t.answer() == null || t.answer().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.faq_answer_required");
            }

            if (t.category() == null || t.category().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "error.faq_category_required");
            }
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