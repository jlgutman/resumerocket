package com.resumerocket.resume;

import java.util.List;

/**
 * The structured resume document persisted as JSON on {@link TailoredResume#getResumeContentJson()}.
 * Consumed by tailoring (as the base to suggest changes against), preview rendering, export
 * (PDF/DOCX/plaintext), and version diffing — see research.md §4.
 */
public record ResumeContent(
    ContactInfoSnapshot contactInfo,
    String summary,
    List<ExperienceItem> experience,
    List<EducationItem> education,
    List<String> skills) {

  public record ContactInfoSnapshot(
      String fullName, String email, String phone, String location, String links) {}

  public record ExperienceItem(
      String company,
      String title,
      String startDate,
      String endDate,
      boolean currentRole,
      List<String> bullets) {}

  public record EducationItem(
      String institution,
      String credential,
      String fieldOfStudy,
      String startDate,
      String endDate,
      String description) {}
}
