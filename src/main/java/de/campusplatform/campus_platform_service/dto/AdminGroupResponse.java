package de.campusplatform.campus_platform_service.dto;

import java.util.List;

public record AdminGroupResponse(
        Long id,
        String name,
        Long courseOfStudyId,
        String courseOfStudyName,
        Long specializationId,
        String specialization,
        int memberCount,
        List<GroupMemberDTO> members
) {
    public record GroupMemberDTO(
            Long id,
            String studentNumber,
            String title,
            String firstName,
            String lastName
    ) {}
}
