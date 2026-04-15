package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.FeedbackRequest;
import de.campusplatform.campus_platform_service.dto.FeedbackResponse;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.Feedback;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import de.campusplatform.campus_platform_service.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final AppUserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, AppUserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void submitFeedback(FeedbackRequest request) {
        AppUser lecturer = userRepository.findById(request.lecturerId())
                .orElseThrow(() -> new AppException("Lecturer not found"));
        
        Feedback feedback = Feedback.builder()
                .content(request.content())
                .targetLecturer(lecturer)
                .createdAt(LocalDateTime.now())
                .build();
        
        feedbackRepository.save(feedback);
    }

    public List<FeedbackResponse> getFeedbackForLecturer(Long lecturerId) {
        return feedbackRepository.findByTargetLecturerIdOrderByCreatedAtDesc(lecturerId).stream()
                .map(f -> new FeedbackResponse(f.getId(), f.getContent(), f.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
