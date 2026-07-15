package com.resumerocket.tailoring;

import com.resumerocket.common.ApiException;
import com.resumerocket.resume.ResumeContent;
import com.resumerocket.resume.ResumeContent.ExperienceItem;
import com.resumerocket.resume.TailoredResume;
import com.resumerocket.resume.TailoredResumeRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuggestionReviewService {

  private final AiSuggestionRepository aiSuggestionRepository;
  private final TailoredResumeRepository tailoredResumeRepository;
  private final TailoringService tailoringService;

  public SuggestionReviewService(
      AiSuggestionRepository aiSuggestionRepository,
      TailoredResumeRepository tailoredResumeRepository,
      TailoringService tailoringService) {
    this.aiSuggestionRepository = aiSuggestionRepository;
    this.tailoredResumeRepository = tailoredResumeRepository;
    this.tailoringService = tailoringService;
  }

  @Transactional
  public AiSuggestionResponse resolve(
      Long userAccountId, Long resumeId, Long suggestionId, SuggestionResolutionRequest request) {
    TailoredResume resume = tailoringService.requireOwned(userAccountId, resumeId);
    AiSuggestion suggestion =
        aiSuggestionRepository
            .findById(suggestionId)
            .filter(s -> s.getTailoredResumeId().equals(resume.getId()))
            .orElseThrow(() -> ApiException.notFound("Suggestion not found"));

    if (request.reviewState() == ReviewState.EDITED
        && (request.finalText() == null || request.finalText().isBlank())) {
      throw ApiException.badRequest("finalText is required when reviewState is EDITED");
    }

    String appliedText =
        switch (request.reviewState()) {
          case ACCEPTED -> suggestion.getSuggestedText();
          case EDITED -> request.finalText();
          case REJECTED, PENDING -> null;
        };

    suggestion.setReviewState(request.reviewState());
    suggestion.setFinalText(appliedText);

    if (appliedText != null) {
      applyToResumeContent(resume, suggestion, appliedText);
      tailoredResumeRepository.save(resume);
    }

    return AiSuggestionResponse.from(suggestion);
  }

  private void applyToResumeContent(TailoredResume resume, AiSuggestion suggestion, String appliedText) {
    ResumeContent content = tailoringService.readResumeContent(resume);
    ResumeContent updated = withResolvedSuggestion(content, suggestion, appliedText);
    resume.setResumeContentJson(tailoringService.writeJson(updated));
  }

  private ResumeContent withResolvedSuggestion(
      ResumeContent content, AiSuggestion suggestion, String appliedText) {
    String target = suggestion.getTargetSection();

    if ("summary".equals(target)) {
      return new ResumeContent(
          content.contactInfo(), appliedText, content.experience(), content.education(), content.skills());
    }

    if ("skills".equals(target)) {
      List<String> skills = new ArrayList<>(content.skills());
      if (!skills.contains(appliedText)) {
        skills.add(appliedText);
      }
      return new ResumeContent(
          content.contactInfo(), content.summary(), content.experience(), content.education(), skills);
    }

    if (target != null && target.startsWith("experience:")) {
      int index = parseIndex(target);
      if (index >= 0 && index < content.experience().size()) {
        List<ExperienceItem> experience = new ArrayList<>(content.experience());
        ExperienceItem item = experience.get(index);
        List<String> bullets = new ArrayList<>(item.bullets());
        int bulletIdx = suggestion.getOriginalText() == null ? -1 : bullets.indexOf(suggestion.getOriginalText());
        if (bulletIdx >= 0) {
          bullets.set(bulletIdx, appliedText);
        } else {
          bullets.add(appliedText);
        }
        experience.set(
            index,
            new ExperienceItem(
                item.company(), item.title(), item.startDate(), item.endDate(), item.currentRole(), bullets));
        return new ResumeContent(
            content.contactInfo(), content.summary(), experience, content.education(), content.skills());
      }
    }

    // EMPHASIS suggestions (or unrecognized targets) don't structurally mutate the document.
    return content;
  }

  private int parseIndex(String target) {
    try {
      return Integer.parseInt(target.substring(target.indexOf(':') + 1));
    } catch (NumberFormatException ex) {
      return -1;
    }
  }
}
