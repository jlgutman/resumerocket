package com.resumerocket.jobdescription;

import com.resumerocket.auth.CurrentUserProvider;
import com.resumerocket.resume.TailoredResumeResponse;
import com.resumerocket.tailoring.TailoringService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job-descriptions")
public class JobDescriptionController {

  private final JobDescriptionService jobDescriptionService;
  private final TailoringService tailoringService;
  private final CurrentUserProvider currentUserProvider;

  public JobDescriptionController(
      JobDescriptionService jobDescriptionService,
      TailoringService tailoringService,
      CurrentUserProvider currentUserProvider) {
    this.jobDescriptionService = jobDescriptionService;
    this.tailoringService = tailoringService;
    this.currentUserProvider = currentUserProvider;
  }

  @PostMapping
  public ResponseEntity<JobDescriptionResponse> submit(@Valid @RequestBody JobDescriptionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(jobDescriptionService.submit(currentUserProvider.require().id(), request));
  }

  @PostMapping("/{id}/tailor")
  public ResponseEntity<TailoredResumeResponse> tailor(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(tailoringService.generateDraft(currentUserProvider.require().id(), id));
  }
}
