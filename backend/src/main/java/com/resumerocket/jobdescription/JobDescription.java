package com.resumerocket.jobdescription;

import com.resumerocket.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_description")
public class JobDescription extends BaseEntity {

  @Column(name = "user_account_id", nullable = false)
  private Long userAccountId;

  @Lob
  @Column(name = "raw_text", nullable = false, length = 16777216)
  private String rawText;

  /** JSON array of extracted keyword/requirement strings, produced by AiTailoringService. */
  @Lob
  @Column(name = "extracted_requirements", length = 16777216)
  private String extractedRequirementsJson;

  protected JobDescription() {}

  public JobDescription(Long userAccountId, String rawText) {
    this.userAccountId = userAccountId;
    this.rawText = rawText;
  }

  public Long getUserAccountId() {
    return userAccountId;
  }

  public String getRawText() {
    return rawText;
  }

  public String getExtractedRequirementsJson() {
    return extractedRequirementsJson;
  }

  public void setExtractedRequirementsJson(String extractedRequirementsJson) {
    this.extractedRequirementsJson = extractedRequirementsJson;
  }
}
