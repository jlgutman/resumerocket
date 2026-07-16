package com.resumerocket.resumeimport;

import com.resumerocket.common.ApiException;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Wraps Apache PDFBox's {@link PDFTextStripper} (research.md #1) and turns the two documented
 * failure modes — an unreadable file and a file with no usable text layer — into the
 * corresponding {@link ApiException}s (research.md #6, FR-008, FR-009) before any AI call is
 * attempted.
 */
@Component
public class PdfTextExtractor {

  /** Below this many non-whitespace characters, treat the PDF as having no extractable text. */
  private static final int MIN_EXTRACTED_TEXT_LENGTH = 40;

  public String extractText(byte[] pdfBytes) {
    try (PDDocument document = Loader.loadPDF(pdfBytes)) {
      String text = new PDFTextStripper().getText(document);
      String trimmed = text == null ? "" : text.strip();
      if (trimmed.length() < MIN_EXTRACTED_TEXT_LENGTH) {
        throw noExtractableText();
      }
      return trimmed;
    } catch (InvalidPasswordException ex) {
      throw new ApiException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "This PDF is password-protected and can't be read. Please upload an unprotected file"
              + " or enter your details manually.");
    } catch (IOException ex) {
      throw new ApiException(
          HttpStatus.UNPROCESSABLE_ENTITY,
          "We couldn't open this PDF — it may be corrupted. Please try a different file or enter"
              + " your details manually.");
    }
  }

  private ApiException noExtractableText() {
    return new ApiException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "This PDF doesn't contain readable text — it may be a scanned image. Please enter your"
            + " details manually.");
  }
}
