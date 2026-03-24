package de.campusplatform.campus_platform_service.dto;

import java.util.List;

public record BulkInvitationRequest(
        List<InvitationRequest> invitations
) {
}
