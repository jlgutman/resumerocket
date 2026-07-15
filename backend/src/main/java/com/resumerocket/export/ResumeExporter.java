package com.resumerocket.export;

import com.resumerocket.resume.ResumeContent;
import com.resumerocket.template.LayoutDescriptor;

/** One implementation per {@link ExportFormat} (FR-013); all render the same {@link ResumeContent}. */
public interface ResumeExporter {

  ExportFormat format();

  byte[] export(ResumeContent content, LayoutDescriptor layoutDescriptor);

  String contentType();
}
