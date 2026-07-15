package com.resumerocket.profile;

import java.time.LocalDate;

public record WorkExperienceEntryResponse(
    Long id,
    String company,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    boolean currentRole,
    String description,
    int displayOrder) {

  public static WorkExperienceEntryResponse from(WorkExperienceEntry entry) {
    return new WorkExperienceEntryResponse(
        entry.getId(),
        entry.getCompany(),
        entry.getTitle(),
        entry.getStartDate(),
        entry.getEndDate(),
        entry.isCurrentRole(),
        entry.getDescription(),
        entry.getDisplayOrder());
  }
}
