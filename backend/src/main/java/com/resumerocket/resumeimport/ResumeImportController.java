package com.resumerocket.resumeimport;

import com.resumerocket.resumeimport.dto.ResumeImportResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Stateless PDF-to-candidate-profile-data extraction endpoint (contracts/rest-api.md). */
@RestController
@RequestMapping("/profile/resume-import")
public class ResumeImportController {

  private final ResumeImportService resumeImportService;

  public ResumeImportController(ResumeImportService resumeImportService) {
    this.resumeImportService = resumeImportService;
  }

  @PostMapping
  public ResumeImportResult importResume(@RequestParam("file") MultipartFile file) {
    return resumeImportService.importResume(file);
  }
}
