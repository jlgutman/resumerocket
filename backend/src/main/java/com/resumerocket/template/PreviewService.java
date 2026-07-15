package com.resumerocket.template;

import com.resumerocket.resume.TailoredResume;
import com.resumerocket.tailoring.TailoringService;
import org.springframework.stereotype.Service;

@Service
public class PreviewService {

  private final TemplateService templateService;
  private final TailoringService tailoringService;

  public PreviewService(TemplateService templateService, TailoringService tailoringService) {
    this.templateService = templateService;
    this.tailoringService = tailoringService;
  }

  public RenderedLayout render(TailoredResume resume, Long templateId) {
    Template template = templateService.requireTemplate(templateId);
    return new RenderedLayout(
        tailoringService.readResumeContent(resume), templateService.readLayoutDescriptor(template));
  }
}
