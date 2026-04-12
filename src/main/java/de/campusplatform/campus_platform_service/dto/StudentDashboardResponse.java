package de.campusplatform.campus_platform_service.dto;

import java.util.List;

public record StudentDashboardResponse(
    String firstName,
    String lastName,
    String courseOfStudyName,
    Double averageGrade,
    Integer ectsEarned,
    Integer ectsTotal,
    long upcomingExamCount,
    List<StudentTodayEventResponse> todayEvents,
    List<StudentNotificationResponse> notifications
) {}
