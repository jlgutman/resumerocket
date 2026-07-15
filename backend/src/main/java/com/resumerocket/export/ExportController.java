package com.resumerocket.export;

import com.resumerocket.auth.CurrentUserProvider;
import com.resumerocket.resume.TailoredResume;
import com.resumerocket.tailoring.TailoringService;
import com.resumerocket.template.PreviewService;
import com.resumerocket.template.RenderedLayout;
import com.resumerocket.template.TemplateResponse;
import com.resumerocket.template.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExportController {

  private final TemplateService templateService;
  private final PreviewService previewService;
  private final ExportService exportService;
  private final TailoringService tailoringService;
  private final CurrentUserProvider currentUserProvider;

  public ExportController(
      TemplateService templateService,
      PreviewService previewService,
      ExportService exportService,
      TailoringService tailoringService,
      CurrentUserProvider currentUserProvider) {
    this.templateService = templateService;
    this.previewService = previewService;
    this.exportService = exportService;
    this.tailoringService = tailoringService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping("/templates")
  public List<TemplateResponse> listTemplates() {
    return templateService.listTemplates();
  }

  @GetMapping("/tailored-resumes/{id}/preview")
  public RenderedLayout preview(@PathVariable Long id, @RequestParam Long templateId) {
    TailoredResume resume = tailoringService.requireOwned(currentUserProvider.require().id(), id);
    return previewService.render(resume, templateId);
  }

  @PostMapping("/tailored-resumes/{id}/export")
  public ResponseEntity<ExportCreatedResponse> export(
      @PathVariable Long id, @Valid @RequestBody ExportRequest request) {
    TailoredResume resume = tailoringService.requireOwned(currentUserProvider.require().id(), id);
    ExportService.ExportResult result =
        exportService.createExport(resume, request.templateId(), request.format());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new ExportCreatedResponse(
                result.exportId(), "/exports/" + result.exportId() + "/download"));
  }

  @GetMapping("/exports/{exportId}/download")
  public ResponseEntity<byte[]> download(@PathVariable Long exportId) {
    ExportService.DownloadResult result =
        exportService.download(currentUserProvider.require().id(), exportId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(result.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
        .body(result.fileBytes());
  }
}
