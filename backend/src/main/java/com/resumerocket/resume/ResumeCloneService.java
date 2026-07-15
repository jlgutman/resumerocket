package com.resumerocket.resume;

import com.resumerocket.tailoring.AiSuggestion;
import com.resumerocket.tailoring.AiSuggestionRepository;
import com.resumerocket.tailoring.TailoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Clones a tailored resume into a new, independently editable draft (FR-018). */
@Service
public class ResumeCloneService {

  private final TailoredResumeRepository tailoredResumeRepository;
  private final AiSuggestionRepository aiSuggestionRepository;
  private final TailoringService tailoringService;

  public ResumeCloneService(
      TailoredResumeRepository tailoredResumeRepository,
      AiSuggestionRepository aiSuggestionRepository,
      TailoringService tailoringService) {
    this.tailoredResumeRepository = tailoredResumeRepository;
    this.aiSuggestionRepository = aiSuggestionRepository;
    this.tailoringService = tailoringService;
  }

  @Transactional
  public TailoredResumeResponse clone(Long userAccountId, Long resumeId) {
    TailoredResume source = tailoringService.requireOwned(userAccountId, resumeId);

    TailoredResume clone =
        new TailoredResume(
            source.getUserAccountId(),
            source.getJobDescriptionId(),
            source.getSourceProfileSnapshotJson(),
            source.getResumeContentJson(),
            source.getName() + " (Copy)");
    clone.setCompany(source.getCompany());
    clone.setJobTitle(source.getJobTitle());
    clone.setUnmatchedRequirementsJson(source.getUnmatchedRequirementsJson());
    clone.setClonedFromId(source.getId());
    tailoredResumeRepository.save(clone);

    for (AiSuggestion original : aiSuggestionRepository.findByTailoredResumeId(source.getId())) {
      AiSuggestion copy =
          new AiSuggestion(
              clone.getId(),
              original.getTargetSection(),
              original.getSuggestionType(),
              original.getOriginalText(),
              original.getSuggestedText());
      copy.setReviewState(original.getReviewState());
      copy.setFinalText(original.getFinalText());
      aiSuggestionRepository.save(copy);
    }

    return tailoringService.toResponse(clone);
  }
}
