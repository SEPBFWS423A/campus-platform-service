package de.campusplatform.campus_platform_service.dto;

import de.campusplatform.campus_platform_service.model.AcademicTitle;
import java.util.List;

public record AdminGroupResponse(
        Long id,
        String name,
        Long courseOfStudyId,
        String courseOfStudyName,
        Long specializationId,
        String specialization,
        int memberCount,
        Integer startYear,
        Integer startQuartal,
        List<GroupMemberDTO> members
) {
    public record GroupMemberDTO(
            Long id,
            String studentNumber,
            AcademicTitle title,
            String firstName,
            String lastName
    ) {}
}
