package com.resumerocket.export;

import com.resumerocket.resume.ResumeContent;
import com.resumerocket.resume.ResumeContent.EducationItem;
import com.resumerocket.resume.ResumeContent.ExperienceItem;
import com.resumerocket.template.LayoutDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

@Service
public class PdfExportService implements ResumeExporter {

  private static final float MARGIN = 50f;
  private static final float LINE_HEIGHT = 14f;
  private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
  private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();

  @Override
  public ExportFormat format() {
    return ExportFormat.pdf;
  }

  @Override
  public String contentType() {
    return "application/pdf";
  }

  @Override
  public byte[] export(ResumeContent content, LayoutDescriptor layoutDescriptor) {
    try (PDDocument document = new PDDocument()) {
      Writer writer = new Writer(document);
      PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

      writer.writeHeading(content.contactInfo().fullName(), boldFont, 18);
      writer.writeLine(contactLine(content), bodyFont, 10);
      writer.newLine();

      for (String section : layoutDescriptor.sectionOrder()) {
        switch (section) {
          case "summary" -> writeSummary(writer, content, bodyFont, boldFont);
          case "experience" -> writeExperience(writer, content.experience(), bodyFont, boldFont);
          case "education" -> writeEducation(writer, content.education(), bodyFont, boldFont);
          case "skills" -> writeSkills(writer, content.skills(), bodyFont, boldFont);
          default -> {
            // Unknown section names are ignored so new templates degrade gracefully.
          }
        }
      }
      writer.close();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.save(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to generate PDF export", ex);
    }
  }

  private void writeSummary(Writer writer, ResumeContent content, PDFont bodyFont, PDFont boldFont)
      throws IOException {
    if (content.summary() == null || content.summary().isBlank()) {
      return;
    }
    writer.writeHeading("Summary", boldFont, 13);
    writer.writeWrapped(content.summary(), bodyFont, 11);
    writer.newLine();
  }

  private void writeExperience(
      Writer writer, List<ExperienceItem> experience, PDFont bodyFont, PDFont boldFont)
      throws IOException {
    if (experience.isEmpty()) {
      return;
    }
    writer.writeHeading("Experience", boldFont, 13);
    for (ExperienceItem item : experience) {
      writer.writeLine(item.title() + " — " + item.company(), boldFont, 11);
      String dates = item.startDate() + " – " + (item.currentRole() ? "Present" : item.endDate());
      writer.writeLine(dates, bodyFont, 9);
      for (String bullet : item.bullets()) {
        writer.writeWrapped("• " + bullet, bodyFont, 10);
      }
      writer.newLine();
    }
  }

  private void writeEducation(
      Writer writer, List<EducationItem> education, PDFont bodyFont, PDFont boldFont)
      throws IOException {
    if (education.isEmpty()) {
      return;
    }
    writer.writeHeading("Education", boldFont, 13);
    for (EducationItem item : education) {
      String title = item.credential() != null ? item.credential() + ", " + item.institution() : item.institution();
      writer.writeLine(title, boldFont, 11);
      if (item.fieldOfStudy() != null && !item.fieldOfStudy().isBlank()) {
        writer.writeLine(item.fieldOfStudy(), bodyFont, 10);
      }
      writer.newLine();
    }
  }

  private void writeSkills(Writer writer, List<String> skills, PDFont bodyFont, PDFont boldFont)
      throws IOException {
    if (skills.isEmpty()) {
      return;
    }
    writer.writeHeading("Skills", boldFont, 13);
    writer.writeWrapped(String.join(" · ", skills), bodyFont, 10);
    writer.newLine();
  }

  private String contactLine(ResumeContent content) {
    List<String> parts = new ArrayList<>();
    var contact = content.contactInfo();
    if (contact.email() != null) parts.add(contact.email());
    if (contact.phone() != null) parts.add(contact.phone());
    if (contact.location() != null) parts.add(contact.location());
    if (contact.links() != null) parts.add(contact.links());
    return String.join("  |  ", parts);
  }

  /** Handles page breaks and text wrapping so callers can just "write" content top to bottom. */
  private static final class Writer {
    private final PDDocument document;
    private PDPageContentStream stream;
    private float cursorY;

    Writer(PDDocument document) throws IOException {
      this.document = document;
      newPage();
    }

    private void newPage() throws IOException {
      if (stream != null) {
        stream.close();
      }
      PDPage page = new PDPage(PDRectangle.LETTER);
      document.addPage(page);
      stream = new PDPageContentStream(document, page);
      cursorY = PAGE_HEIGHT - MARGIN;
    }

    void writeHeading(String text, PDFont font, float size) throws IOException {
      writeLine(text == null ? "" : text, font, size);
    }

    void writeLine(String text, PDFont font, float size) throws IOException {
      ensureSpace();
      stream.beginText();
      stream.setFont(font, size);
      stream.newLineAtOffset(MARGIN, cursorY);
      stream.showText(sanitize(text));
      stream.endText();
      cursorY -= LINE_HEIGHT;
    }

    void writeWrapped(String text, PDFont font, float size) throws IOException {
      float maxWidth = PAGE_WIDTH - 2 * MARGIN;
      for (String line : wrap(sanitize(text), font, size, maxWidth)) {
        writeLine(line, font, size);
      }
    }

    void newLine() {
      cursorY -= LINE_HEIGHT / 2;
    }

    private void ensureSpace() throws IOException {
      if (cursorY < MARGIN) {
        newPage();
      }
    }

    void close() throws IOException {
      stream.close();
    }

    private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
      List<String> lines = new ArrayList<>();
      StringBuilder current = new StringBuilder();
      for (String word : text.split("\\s+")) {
        String candidate = current.isEmpty() ? word : current + " " + word;
        if (font.getStringWidth(candidate) / 1000 * size > maxWidth && !current.isEmpty()) {
          lines.add(current.toString());
          current = new StringBuilder(word);
        } else {
          current = new StringBuilder(candidate);
        }
      }
      if (!current.isEmpty()) {
        lines.add(current.toString());
      }
      return lines;
    }

    private String sanitize(String text) {
      // WinAnsiEncoding (Standard14Fonts) cannot represent characters like curly quotes.
      return text.replaceAll("[^\\x00-\\xFF]", "?");
    }
  }
}
