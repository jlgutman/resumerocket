package com.resumerocket.resumeimport;

import com.resumerocket.resumeimport.dto.ResumeImportResult;

/**
 * Provider-agnostic boundary around whichever LLM structures raw resume text into candidate
 * profile fields (research.md #2), mirroring {@link com.resumerocket.tailoring.AiTailoringService}.
 */
public interface AiResumeExtractionService {

  /**
   * Turns extracted PDF text into structured candidate profile data. The returned result's {@code
   * sourceFileName} is left null — the caller ({@link ResumeImportService}) fills it in.
   */
  ResumeImportResult extract(String resumeText);
}
