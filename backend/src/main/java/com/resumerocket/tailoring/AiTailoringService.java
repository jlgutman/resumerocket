package com.resumerocket.tailoring;

import com.resumerocket.resume.ResumeContent;
import java.util.List;

/**
 * Provider-agnostic boundary around whichever LLM performs job-description analysis and resume
 * tailoring (research.md §1). Swapping vendors means adding a new implementation, not touching
 * {@link TailoringService} or {@link com.resumerocket.jobdescription.JobDescriptionService}.
 */
public interface AiTailoringService {

  /** Extracts key requirements/keywords from a pasted job description (FR-006). */
  List<String> extractRequirements(String jobDescriptionText);

  /**
   * Produces tailoring suggestions for {@code baseResume} against the given job description and
   * previously extracted requirements (FR-007-FR-009, FR-020).
   */
  TailoringResult tailor(
      ResumeContent baseResume, String jobDescriptionText, List<String> extractedRequirements);
}
