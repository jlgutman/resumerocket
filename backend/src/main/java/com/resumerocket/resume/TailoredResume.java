package com.resumerocket.resume;

import com.resumerocket.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "tailored_resume")
public class TailoredResume extends BaseEntity {

  @Column(name = "user_account_id", nullable = false)
  private Long userAccountId;

  @Column(name = "job_description_id")
  private Long jobDescriptionId;

  /** Frozen copy of the master profile content used to build this version (FR-004). */
  @Lob
  @Column(name = "source_profile_snapshot", nullable = false, length = 16777216)
  private String sourceProfileSnapshotJson;

  /** The current resume document (see {@link ResumeContent}), evolves as suggestions resolve. */
  @Lob
  @Column(name = "resume_content", nullable = false, length = 16777216)
  private String resumeContentJson;

  /** JSON array of job requirements that could not be matched to profile content (FR-020). */
  @Lob
  @Column(name = "unmatched_requirements", length = 16777216)
  private String unmatchedRequirementsJson;

  @Column(nullable = false)
  private String name;

  private String company;

  @Column(name = "job_title")
  private String jobTitle;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ResumeStatus status = ResumeStatus.DRAFT;

  @Column(name = "cloned_from_id")
  private Long clonedFromId;

  @Column(name = "regenerated_from_id")
  private Long regeneratedFromId;

  protected TailoredResume() {}

  public TailoredResume(
      Long userAccountId,
      Long jobDescriptionId,
      String sourceProfileSnapshotJson,
      String resumeContentJson,
      String name) {
    this.userAccountId = userAccountId;
    this.jobDescriptionId = jobDescriptionId;
    this.sourceProfileSnapshotJson = sourceProfileSnapshotJson;
    this.resumeContentJson = resumeContentJson;
    this.name = name;
  }

  public Long getUserAccountId() {
    return userAccountId;
  }

  public Long getJobDescriptionId() {
    return jobDescriptionId;
  }

  public String getSourceProfileSnapshotJson() {
    return sourceProfileSnapshotJson;
  }

  public String getResumeContentJson() {
    return resumeContentJson;
  }

  public void setResumeContentJson(String resumeContentJson) {
    this.resumeContentJson = resumeContentJson;
  }

  public String getUnmatchedRequirementsJson() {
    return unmatchedRequirementsJson;
  }

  public void setUnmatchedRequirementsJson(String unmatchedRequirementsJson) {
    this.unmatchedRequirementsJson = unmatchedRequirementsJson;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getJobTitle() {
    return jobTitle;
  }

  public void setJobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public ResumeStatus getStatus() {
    return status;
  }

  public void setStatus(ResumeStatus status) {
    this.status = status;
  }

  public Long getClonedFromId() {
    return clonedFromId;
  }

  public void setClonedFromId(Long clonedFromId) {
    this.clonedFromId = clonedFromId;
  }

  public Long getRegeneratedFromId() {
    return regeneratedFromId;
  }

  public void setRegeneratedFromId(Long regeneratedFromId) {
    this.regeneratedFromId = regeneratedFromId;
  }
}
