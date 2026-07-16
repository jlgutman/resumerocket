package com.resumerocket.resumeimport.dto;

import java.util.List;

/**
 * Response body of {@code POST /profile/resume-import}. Never persisted (research.md #3) — held
 * client-side during the review step until the user confirms via the existing profile mutation
 * endpoints, or discards it.
 */
public record ResumeImportResult(
    String sourceFileName,
    ContactInfoCandidate contactInfo,
    List<WorkExperienceCandidate> workExperience,
    List<EducationCandidate> education,
    List<SkillCandidate> skills,
    List<String> warnings) {

  public ResumeImportResult withSourceFileName(String sourceFileName) {
    return new ResumeImportResult(
        sourceFileName, contactInfo, workExperience, education, skills, warnings);
  }
}
