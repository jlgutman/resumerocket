package com.resumerocket.resume;

import com.resumerocket.common.ApiException;
import com.resumerocket.tailoring.TailoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regenerates a tailored resume from the current master profile while preserving the original
 * version (FR-019). Requires the source resume to have an associated job description to retailor
 * against.
 */
@Service
public class ResumeRegenerateService {

  private final TailoredResumeRepository tailoredResumeRepository;
  private final TailoringService tailoringService;

  public ResumeRegenerateService(
      TailoredResumeRepository tailoredResumeRepository, TailoringService tailoringService) {
    this.tailoredResumeRepository = tailoredResumeRepository;
    this.tailoringService = tailoringService;
  }

  @Transactional
  public TailoredResumeResponse regenerate(Long userAccountId, Long resumeId) {
    TailoredResume source = tailoringService.requireOwned(userAccountId, resumeId);
    if (source.getJobDescriptionId() == null) {
      throw ApiException.badRequest(
          "This resume has no associated job description to regenerate against");
    }

    TailoredResumeResponse regeneratedResponse =
        tailoringService.generateDraft(userAccountId, source.getJobDescriptionId());
    TailoredResume regenerated = tailoredResumeRepository.getReferenceById(regeneratedResponse.id());
    regenerated.setName(source.getName());
    regenerated.setCompany(source.getCompany());
    regenerated.setJobTitle(source.getJobTitle());
    regenerated.setRegeneratedFromId(source.getId());
    tailoredResumeRepository.save(regenerated);

    return tailoringService.toResponse(regenerated);
  }
}
