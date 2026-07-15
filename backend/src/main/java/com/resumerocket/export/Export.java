package com.resumerocket.export;

import com.resumerocket.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "export")
public class Export extends BaseEntity {

  @Column(name = "tailored_resume_id", nullable = false)
  private Long tailoredResumeId;

  @Column(name = "template_id", nullable = false)
  private Long templateId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private ExportFormat format;

  @Column(name = "file_reference", nullable = false, length = 500)
  private String fileReference;

  protected Export() {}

  public Export(Long tailoredResumeId, Long templateId, ExportFormat format, String fileReference) {
    this.tailoredResumeId = tailoredResumeId;
    this.templateId = templateId;
    this.format = format;
    this.fileReference = fileReference;
  }

  public Long getTailoredResumeId() {
    return tailoredResumeId;
  }

  public Long getTemplateId() {
    return templateId;
  }

  public ExportFormat getFormat() {
    return format;
  }

  public String getFileReference() {
    return fileReference;
  }
}
