package com.resumerocket.profile;

import java.time.LocalDate;

public record EducationEntryResponse(
    Long id,
    String institution,
    String credential,
    String fieldOfStudy,
    LocalDate startDate,
    LocalDate endDate,
    String description,
    int displayOrder) {

  public static EducationEntryResponse from(EducationEntry entry) {
    return new EducationEntryResponse(
        entry.getId(),
        entry.getInstitution(),
        entry.getCredential(),
        entry.getFieldOfStudy(),
        entry.getStartDate(),
        entry.getEndDate(),
        entry.getDescription(),
        entry.getDisplayOrder());
  }
}
