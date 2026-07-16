package com.resumerocket.resumeimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.resumerocket.common.ApiException;
import com.resumerocket.resumeimport.dto.ContactInfoCandidate;
import com.resumerocket.resumeimport.dto.ResumeImportResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

/** Orchestration tests for {@link ResumeImportService}; {@link AiResumeExtractionService} is
 * mocked so this never calls OpenAI and doesn't require OPENAI_API_KEY (CLAUDE.md testing
 * conventions). */
@ExtendWith(MockitoExtension.class)
class ResumeImportServiceTest {

  @Mock private PdfTextExtractor pdfTextExtractor;
  @Mock private AiResumeExtractionService aiResumeExtractionService;

  private ResumeImportService service;

  @BeforeEach
  void setUp() {
    service = new ResumeImportService(pdfTextExtractor, aiResumeExtractionService);
  }

  @Test
  void extractsAndReturnsCandidateDataForAValidPdf() {
    MockMultipartFile file =
        new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-1.4 ...".getBytes());
    when(pdfTextExtractor.extractText(any())).thenReturn("Jane Doe, Software Engineer");
    ResumeImportResult extracted =
        new ResumeImportResult(
            null,
            new ContactInfoCandidate("Jane Doe", null, null, null, null, List.of()),
            List.of(),
            List.of(),
            List.of(),
            List.of());
    when(aiResumeExtractionService.extract("Jane Doe, Software Engineer")).thenReturn(extracted);

    ResumeImportResult result = service.importResume(file);

    assertThat(result.sourceFileName()).isEqualTo("resume.pdf");
    assertThat(result.contactInfo().fullName()).isEqualTo("Jane Doe");
  }

  @Test
  void rejectsANonPdfContentType() {
    MockMultipartFile file =
        new MockMultipartFile("file", "resume.docx", "application/msword", "not a pdf".getBytes());

    assertThatThrownBy(() -> service.importResume(file))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
        .hasMessageContaining("Only PDF files");
    verifyNoInteractions(pdfTextExtractor, aiResumeExtractionService);
  }

  @Test
  void rejectsAnEmptyUpload() {
    MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);

    assertThatThrownBy(() -> service.importResume(file))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    verifyNoInteractions(pdfTextExtractor, aiResumeExtractionService);
  }

  @Test
  void rejectsANullUpload() {
    assertThatThrownBy(() -> service.importResume(null))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    verifyNoInteractions(pdfTextExtractor, aiResumeExtractionService);
  }

  @Test
  void propagatesTheExtractorsUnreadablePdfError() {
    MockMultipartFile file =
        new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-1.4 ...".getBytes());
    when(pdfTextExtractor.extractText(any()))
        .thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "unreadable"));

    assertThatThrownBy(() -> service.importResume(file))
        .isInstanceOf(ApiException.class)
        .satisfies(
            ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    verifyNoInteractions(aiResumeExtractionService);
  }

  @Test
  void propagatesTheAiServicesFailure() {
    MockMultipartFile file =
        new MockMultipartFile("file", "resume.pdf", "application/pdf", "%PDF-1.4 ...".getBytes());
    when(pdfTextExtractor.extractText(any())).thenReturn("some text");
    when(aiResumeExtractionService.extract("some text"))
        .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "extraction failed"));

    assertThatThrownBy(() -> service.importResume(file))
        .isInstanceOf(ApiException.class)
        .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
  }
}
