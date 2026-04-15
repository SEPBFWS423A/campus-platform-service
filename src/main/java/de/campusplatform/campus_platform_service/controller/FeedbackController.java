package de.campusplatform.campus_platform_service.controller;

import de.campusplatform.campus_platform_service.dto.FeedbackRequest;
import de.campusplatform.campus_platform_service.dto.FeedbackResponse;
import de.campusplatform.campus_platform_service.security.CustomUserDetails;
import de.campusplatform.campus_platform_service.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<Void> submitFeedback(@RequestBody FeedbackRequest request) {
        feedbackService.submitFeedback(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('LECTURER')")
    public ResponseEntity<List<FeedbackResponse>> getMyFeedback(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(feedbackService.getFeedbackForLecturer(userDetails.appUser().getId()));
    }
}
