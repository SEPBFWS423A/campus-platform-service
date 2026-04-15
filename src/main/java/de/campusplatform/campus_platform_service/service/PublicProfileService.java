package de.campusplatform.campus_platform_service.service;

import de.campusplatform.campus_platform_service.dto.PublicProfileRequest;
import de.campusplatform.campus_platform_service.dto.PublicProfileResponse;
import de.campusplatform.campus_platform_service.exception.AppException;
import de.campusplatform.campus_platform_service.model.AppUser;
import de.campusplatform.campus_platform_service.model.PublicProfile;
import de.campusplatform.campus_platform_service.repository.PublicProfileRepository;
import de.campusplatform.campus_platform_service.repository.AppUserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PublicProfileService {

    private final PublicProfileRepository publicProfileRepository;
    private final AppUserRepository userRepository;

    public PublicProfileService(PublicProfileRepository publicProfileRepository, AppUserRepository userRepository) {
        this.publicProfileRepository = publicProfileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PublicProfileResponse> getProfileByEmail(String email) {
        return publicProfileRepository.findByAppUserEmail(email)
                .map(this::mapToResponse);
    }

    @Transactional
    public PublicProfileResponse createOrUpdateProfile(String email, PublicProfileRequest request) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found"));

        PublicProfile profile = publicProfileRepository.findByAppUserEmail(email)
                .orElseGet(() -> {
                    PublicProfile newProfile = new PublicProfile();
                    newProfile.setAppUser(user);
                    return newProfile;
                });

        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getInterests() != null) profile.setInterests(request.getInterests());
        if (request.getHobbies() != null) profile.setHobbies(request.getHobbies());
        if (request.getSkills() != null) profile.setSkills(request.getSkills());
        if (request.getVisibility() != null) profile.setVisibility(request.getVisibility());

        PublicProfile saved = publicProfileRepository.save(profile);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PublicProfileResponse> searchProfiles(String searchInput, String currentUserEmail) {
        List<PublicProfile> allProfiles = publicProfileRepository.findAllByVisibilityTrueAndAppUserEmailNot(currentUserEmail);
        
        if (searchInput == null || searchInput.isBlank()) {
            return allProfiles.stream().map(this::mapToResponse).collect(Collectors.toList());
        }

        String[] keywords = searchInput.toLowerCase().trim().split("\\s+");
        
        return allProfiles.stream()
                .map(profile -> new ScoredProfile(profile, calculateScore(profile, keywords)))
                .filter(scored -> scored.getScore() > 0)
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .map(scored -> mapToResponse(scored.getProfile()))
                .collect(Collectors.toList());
    }

    private int calculateScore(PublicProfile profile, String[] keywords) {
        int totalScore = 0;
        String firstName = profile.getAppUser().getFirstName() != null ? profile.getAppUser().getFirstName().toLowerCase() : "";
        String lastName = profile.getAppUser().getLastName() != null ? profile.getAppUser().getLastName().toLowerCase() : "";
        String bio = profile.getBio() != null ? profile.getBio().toLowerCase() : "";
        String interests = profile.getInterests() != null ? profile.getInterests().toLowerCase() : "";
        String hobbies = profile.getHobbies() != null ? profile.getHobbies().toLowerCase() : "";
        String skills = profile.getSkills() != null ? profile.getSkills().toLowerCase() : "";

        for (String word : keywords) {
            if (firstName.contains(word) || lastName.contains(word)) totalScore += 50;
            if (interests.contains(word)) totalScore += 30;
            if (hobbies.contains(word)) totalScore += 30;
            if (skills.contains(word)) totalScore += 30;
            if (bio.contains(word)) totalScore += 10;
        }
        return totalScore;
    }

    @Getter
    @AllArgsConstructor
    private static class ScoredProfile {
        private final PublicProfile profile;
        private final int score;
    }

    private PublicProfileResponse mapToResponse(PublicProfile profile) {
        AppUser user = profile.getAppUser();
        String studyGroupName = "";
        // Extract study group if available from studentProfile memberships
        if (user.getStudentProfile() != null && user.getStudentProfile().getMemberships() != null && !user.getStudentProfile().getMemberships().isEmpty()) {
            studyGroupName = user.getStudentProfile().getMemberships().iterator().next().getStudyGroup().getName();
        }

        return PublicProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .studyGroupName(studyGroupName)
                .bio(profile.getBio())
                .interests(profile.getInterests())
                .hobbies(profile.getHobbies())
                .skills(profile.getSkills())
                .visibility(profile.isVisibility())
                .build();
    }
}
