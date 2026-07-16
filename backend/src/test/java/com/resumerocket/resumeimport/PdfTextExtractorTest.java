package com.resumerocket.resumeimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.resumerocket.common.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PdfTextExtractorTest {

  private final PdfTextExtractor extractor = new PdfTextExtractor();

  @Test
  void extractsTextFromAWellFormedPdf() throws IOException {
    byte[] pdf = pdfWithText("Jane Doe\nSoftware Engineer at Acme Corp, 2020-2024.");

    String text = extractor.extractText(pdf);

    assertThat(text).contains("Jane Doe").contains("Acme Corp");
  }

  @Test
  void rejectsAPdfWithNoExtractableText() throws IOException {
    byte[] pdf = pdfWithNoContentStream();

    assertThatThrownBy(() -> extractor.extractText(pdf))
        .isInstanceOf(ApiException.class)
        .satisfies(
            ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
        .hasMessageContaining("scanned image");
  }

  @Test
  void rejectsACorruptedFile() {
    byte[] notAPdf = "this is definitely not a pdf file".getBytes();

    assertThatThrownBy(() -> extractor.extractText(notAPdf))
        .isInstanceOf(ApiException.class)
        .satisfies(
            ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
        .hasMessageContaining("couldn't open this PDF");
  }

  @Test
  void rejectsAPasswordProtectedPdf() throws IOException {
    byte[] pdf = passwordProtectedPdf();

    assertThatThrownBy(() -> extractor.extractText(pdf))
        .isInstanceOf(ApiException.class)
        .satisfies(
            ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
        .hasMessageContaining("password-protected");
  }

  private byte[] pdfWithText(String text) throws IOException {
    try (PDDocument document = new PDDocument()) {
      PDPage page = new PDPage();
      document.addPage(page);
      try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        stream.newLineAtOffset(50, 700);
        for (String line : text.split("\n")) {
          stream.showText(line);
          stream.newLineAtOffset(0, -14);
        }
        stream.endText();
      }
      return toBytes(document);
    }
  }

  private byte[] pdfWithNoContentStream() throws IOException {
    try (PDDocument document = new PDDocument()) {
      document.addPage(new PDPage());
      return toBytes(document);
    }
  }

  private byte[] passwordProtectedPdf() throws IOException {
    try (PDDocument document = new PDDocument()) {
      document.addPage(new PDPage());
      AccessPermission permission = new AccessPermission();
      StandardProtectionPolicy policy =
          new StandardProtectionPolicy("owner-secret", "user-secret", permission);
      document.protect(policy);
      return toBytes(document);
    }
  }

  private byte[] toBytes(PDDocument document) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    document.save(out);
    return out.toByteArray();
  }
}
