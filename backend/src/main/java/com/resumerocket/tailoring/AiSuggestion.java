package com.resumerocket.tailoring;

import com.resumerocket.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_suggestion")
public class AiSuggestion extends BaseEntity {

  @Column(name = "tailored_resume_id", nullable = false)
  private Long tailoredResumeId;

  /** Location key within the resume content, e.g. "experience:0", "skills", "summary". */
  @Column(name = "target_section", nullable = false, length = 50)
  private String targetSection;

  @Enumerated(EnumType.STRING)
  @Column(name = "suggestion_type", nullable = false, length = 50)
  private SuggestionType suggestionType;

  @Lob
  @Column(name = "original_text", length = 65535)
  private String originalText;

  @Lob
  @Column(name = "suggested_text", nullable = false, length = 65535)
  private String suggestedText;

  @Lob
  @Column(name = "final_text", length = 65535)
  private String finalText;

  @Enumerated(EnumType.STRING)
  @Column(name = "review_state", nullable = false, length = 20)
  private ReviewState reviewState = ReviewState.PENDING;

  protected AiSuggestion() {}

  public AiSuggestion(
      Long tailoredResumeId,
      String targetSection,
      SuggestionType suggestionType,
      String originalText,
      String suggestedText) {
    this.tailoredResumeId = tailoredResumeId;
    this.targetSection = targetSection;
    this.suggestionType = suggestionType;
    this.originalText = originalText;
    this.suggestedText = suggestedText;
  }

  public Long getTailoredResumeId() {
    return tailoredResumeId;
  }

  public String getTargetSection() {
    return targetSection;
  }

  public SuggestionType getSuggestionType() {
    return suggestionType;
  }

  public String getOriginalText() {
    return originalText;
  }

  public String getSuggestedText() {
    return suggestedText;
  }

  public String getFinalText() {
    return finalText;
  }

  public void setFinalText(String finalText) {
    this.finalText = finalText;
  }

  public ReviewState getReviewState() {
    return reviewState;
  }

  public void setReviewState(ReviewState reviewState) {
    this.reviewState = reviewState;
  }
}
