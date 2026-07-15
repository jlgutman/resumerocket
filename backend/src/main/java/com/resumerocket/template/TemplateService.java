package com.resumerocket.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumerocket.common.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

  private final TemplateRepository templateRepository;
  private final ObjectMapper objectMapper;

  public TemplateService(TemplateRepository templateRepository, ObjectMapper objectMapper) {
    this.templateRepository = templateRepository;
    this.objectMapper = objectMapper;
  }

  public List<TemplateResponse> listTemplates() {
    return templateRepository.findAll().stream().map(this::toResponse).toList();
  }

  public Template requireTemplate(Long id) {
    return templateRepository.findById(id).orElseThrow(() -> ApiException.notFound("Template not found"));
  }

  public LayoutDescriptor readLayoutDescriptor(Template template) {
    try {
      return objectMapper.readValue(template.getLayoutDescriptorJson(), LayoutDescriptor.class);
    } catch (Exception ex) {
      throw new IllegalStateException("Corrupt layout descriptor for template " + template.getId(), ex);
    }
  }

  private TemplateResponse toResponse(Template template) {
    return new TemplateResponse(template.getId(), template.getName(), readLayoutDescriptor(template));
  }
}
