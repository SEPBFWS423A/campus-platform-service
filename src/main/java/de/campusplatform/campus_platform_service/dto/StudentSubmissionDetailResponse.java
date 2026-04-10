package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.enums.SubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record StudentSubmissionDetailResponse(
        Long submissionId,
        Long courseSeriesId,
        String examTypeName,
        SubmissionStatus status,
        LocalDateTime submissionStartDate,
        LocalDateTime submissionDeadline,
        LocalDateTime submissionDate,
        boolean hasDocuments,
        boolean missingDocuments,
        boolean editable,
        boolean finalSubmitAllowed,
        Double grade,
        Double points,
        String feedback,
        List<SubmissionDocumentResponse> documents
) {
}
