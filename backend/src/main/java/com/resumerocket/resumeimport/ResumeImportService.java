package com.resumerocket.resumeimport;

import com.resumerocket.common.ApiException;
import com.resumerocket.resumeimport.dto.ResumeImportResult;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Orchestrates {@code POST /profile/resume-import}: validate the upload, extract its text, then
 * structure that text via {@link AiResumeExtractionService}. Stateless — nothing is persisted
 * here (research.md #3); the file-size ceiling itself is enforced by Spring's multipart
 * configuration before a request ever reaches this service (see {@code
 * GlobalExceptionHandler#handleUploadTooLarge}).
 */
@Service
public class ResumeImportService {

  private static final String SUPPORTED_CONTENT_TYPE = "application/pdf";

  private final PdfTextExtractor pdfTextExtractor;
  private final AiResumeExtractionService aiResumeExtractionService;

  public ResumeImportService(
      PdfTextExtractor pdfTextExtractor, AiResumeExtractionService aiResumeExtractionService) {
    this.pdfTextExtractor = pdfTextExtractor;
    this.aiResumeExtractionService = aiResumeExtractionService;
  }

  public ResumeImportResult importResume(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose a PDF file to upload.");
    }
    if (!SUPPORTED_CONTENT_TYPE.equals(file.getContentType())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF files are supported.");
    }

    String resumeText = pdfTextExtractor.extractText(readBytes(file));
    ResumeImportResult extracted = aiResumeExtractionService.extract(resumeText);
    return extracted.withSourceFileName(file.getOriginalFilename());
  }

  private byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "We couldn't read the uploaded file.");
    }
  }
}
