package com.resumerocket.export;

import com.resumerocket.common.ApiException;
import com.resumerocket.common.AppProperties;
import com.resumerocket.resume.ResumeContent;
import com.resumerocket.resume.TailoredResume;
import com.resumerocket.tailoring.TailoringService;
import com.resumerocket.template.LayoutDescriptor;
import com.resumerocket.template.Template;
import com.resumerocket.template.TemplateService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {

  private final ExportRepository exportRepository;
  private final TailoringService tailoringService;
  private final TemplateService templateService;
  private final AppProperties appProperties;
  private final Map<ExportFormat, ResumeExporter> exportersByFormat;

  public ExportService(
      ExportRepository exportRepository,
      TailoringService tailoringService,
      TemplateService templateService,
      AppProperties appProperties,
      java.util.List<ResumeExporter> exporters) {
    this.exportRepository = exportRepository;
    this.tailoringService = tailoringService;
    this.templateService = templateService;
    this.appProperties = appProperties;
    this.exportersByFormat = exporters.stream().collect(Collectors.toMap(ResumeExporter::format, Function.identity()));
  }

  @Transactional
  public ExportResult createExport(TailoredResume resume, Long templateId, ExportFormat format) {
    Template template = templateService.requireTemplate(templateId);
    ResumeExporter exporter = exportersByFormat.get(format);
    if (exporter == null) {
      throw ApiException.badRequest("Unsupported export format: " + format);
    }
    ResumeContent content = tailoringService.readResumeContent(resume);
    LayoutDescriptor layoutDescriptor = templateService.readLayoutDescriptor(template);
    byte[] fileBytes = exporter.export(content, layoutDescriptor);

    String fileName = UUID.randomUUID() + "." + format;
    Path storageDir = Path.of(appProperties.getExport().getStorageDir());
    try {
      Files.createDirectories(storageDir);
      Files.write(storageDir.resolve(fileName), fileBytes);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to persist export file", ex);
    }

    Export export = new Export(resume.getId(), templateId, format, fileName);
    exportRepository.save(export);
    return new ExportResult(export.getId(), fileBytes, exporter.contentType(), fileNameFor(resume, format));
  }

  public DownloadResult download(Long userAccountId, Long exportId) {
    Export export =
        exportRepository.findById(exportId).orElseThrow(() -> ApiException.notFound("Export not found"));
    // Verifies the export's resume belongs to the caller before releasing the file (FR-001).
    tailoringService.requireOwned(userAccountId, export.getTailoredResumeId());
    ResumeExporter exporter = exportersByFormat.get(export.getFormat());
    Path filePath = Path.of(appProperties.getExport().getStorageDir()).resolve(export.getFileReference());
    try {
      byte[] bytes = Files.readAllBytes(filePath);
      return new DownloadResult(bytes, exporter.contentType(), export.getFileReference());
    } catch (IOException ex) {
      throw ApiException.notFound("Export file is no longer available");
    }
  }

  private String fileNameFor(TailoredResume resume, ExportFormat format) {
    String base = resume.getName().replaceAll("[^a-zA-Z0-9-_]", "_");
    return base + "." + format;
  }

  public record ExportResult(Long exportId, byte[] fileBytes, String contentType, String fileName) {}

  public record DownloadResult(byte[] fileBytes, String contentType, String fileName) {}
}
