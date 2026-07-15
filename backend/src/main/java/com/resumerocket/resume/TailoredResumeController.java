package com.resumerocket.resume;

import com.resumerocket.auth.CurrentUserProvider;
import com.resumerocket.tailoring.AiSuggestionResponse;
import com.resumerocket.tailoring.SuggestionResolutionRequest;
import com.resumerocket.tailoring.SuggestionReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tailored-resumes")
public class TailoredResumeController {

  private final TailoredResumeService tailoredResumeService;
  private final SuggestionReviewService suggestionReviewService;
  private final ResumeCloneService resumeCloneService;
  private final ResumeRegenerateService resumeRegenerateService;
  private final CurrentUserProvider currentUserProvider;

  public TailoredResumeController(
      TailoredResumeService tailoredResumeService,
      SuggestionReviewService suggestionReviewService,
      ResumeCloneService resumeCloneService,
      ResumeRegenerateService resumeRegenerateService,
      CurrentUserProvider currentUserProvider) {
    this.tailoredResumeService = tailoredResumeService;
    this.suggestionReviewService = suggestionReviewService;
    this.resumeCloneService = resumeCloneService;
    this.resumeRegenerateService = resumeRegenerateService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  public List<TailoredResumeSummary> list(@RequestParam(required = false) String company) {
    return tailoredResumeService.list(currentUserProvider.require().id(), company);
  }

  @GetMapping("/compare")
  public CompareResponse compare(@RequestParam Long leftId, @RequestParam Long rightId) {
    return tailoredResumeService.compare(currentUserProvider.require().id(), leftId, rightId);
  }

  @GetMapping("/{id}")
  public TailoredResumeResponse get(@PathVariable Long id) {
    return tailoredResumeService.get(currentUserProvider.require().id(), id);
  }

  @PatchMapping("/{id}")
  public TailoredResumeResponse update(
      @PathVariable Long id, @RequestBody UpdateTailoredResumeRequest request) {
    return tailoredResumeService.update(currentUserProvider.require().id(), id, request);
  }

  @PatchMapping("/{id}/suggestions/{suggestionId}")
  public AiSuggestionResponse resolveSuggestion(
      @PathVariable Long id,
      @PathVariable Long suggestionId,
      @Valid @RequestBody SuggestionResolutionRequest request) {
    return suggestionReviewService.resolve(
        currentUserProvider.require().id(), id, suggestionId, request);
  }

  @PostMapping("/{id}/clone")
  public ResponseEntity<TailoredResumeResponse> clone(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(resumeCloneService.clone(currentUserProvider.require().id(), id));
  }

  @PostMapping("/{id}/regenerate")
  public ResponseEntity<TailoredResumeResponse> regenerate(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(resumeRegenerateService.regenerate(currentUserProvider.require().id(), id));
  }
}
