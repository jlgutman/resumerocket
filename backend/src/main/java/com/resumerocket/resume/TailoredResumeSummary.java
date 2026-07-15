package com.resumerocket.resume;

import java.time.Instant;

/** Lightweight projection for list views (FR-016). */
public record TailoredResumeSummary(
    Long id, String name, String company, String jobTitle, ResumeStatus status, Instant createdAt) {

  public static TailoredResumeSummary from(TailoredResume resume) {
    return new TailoredResumeSummary(
        resume.getId(),
        resume.getName(),
        resume.getCompany(),
        resume.getJobTitle(),
        resume.getStatus(),
        resume.getCreatedAt());
  }
}
