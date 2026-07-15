package com.resumerocket.export;

import com.resumerocket.resume.ResumeContent;
import com.resumerocket.resume.ResumeContent.EducationItem;
import com.resumerocket.resume.ResumeContent.ExperienceItem;
import com.resumerocket.template.LayoutDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

@Service
public class DocxExportService implements ResumeExporter {

  @Override
  public ExportFormat format() {
    return ExportFormat.docx;
  }

  @Override
  public String contentType() {
    return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  }

  @Override
  public byte[] export(ResumeContent content, LayoutDescriptor layoutDescriptor) {
    try (XWPFDocument document = new XWPFDocument()) {
      addHeading(document, content.contactInfo().fullName(), 20, ParagraphAlignment.CENTER);
      addParagraph(document, contactLine(content), 10, false, ParagraphAlignment.CENTER);

      for (String section : layoutDescriptor.sectionOrder()) {
        switch (section) {
          case "summary" -> writeSummary(document, content);
          case "experience" -> writeExperience(document, content.experience());
          case "education" -> writeEducation(document, content.education());
          case "skills" -> writeSkills(document, content.skills());
          default -> {
            // Unknown section names are ignored so new templates degrade gracefully.
          }
        }
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.write(out);
      return out.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to generate DOCX export", ex);
    }
  }

  private void writeSummary(XWPFDocument document, ResumeContent content) {
    if (content.summary() == null || content.summary().isBlank()) {
      return;
    }
    addHeading(document, "Summary", 14, ParagraphAlignment.LEFT);
    addParagraph(document, content.summary(), 11, false, ParagraphAlignment.LEFT);
  }

  private void writeExperience(XWPFDocument document, List<ExperienceItem> experience) {
    if (experience.isEmpty()) {
      return;
    }
    addHeading(document, "Experience", 14, ParagraphAlignment.LEFT);
    for (ExperienceItem item : experience) {
      addParagraph(document, item.title() + " — " + item.company(), 12, true, ParagraphAlignment.LEFT);
      String dates = item.startDate() + " – " + (item.currentRole() ? "Present" : item.endDate());
      addParagraph(document, dates, 9, false, ParagraphAlignment.LEFT);
      for (String bullet : item.bullets()) {
        addBullet(document, bullet);
      }
    }
  }

  private void writeEducation(XWPFDocument document, List<EducationItem> education) {
    if (education.isEmpty()) {
      return;
    }
    addHeading(document, "Education", 14, ParagraphAlignment.LEFT);
    for (EducationItem item : education) {
      String title = item.credential() != null ? item.credential() + ", " + item.institution() : item.institution();
      addParagraph(document, title, 12, true, ParagraphAlignment.LEFT);
      if (item.fieldOfStudy() != null && !item.fieldOfStudy().isBlank()) {
        addParagraph(document, item.fieldOfStudy(), 10, false, ParagraphAlignment.LEFT);
      }
    }
  }

  private void writeSkills(XWPFDocument document, List<String> skills) {
    if (skills.isEmpty()) {
      return;
    }
    addHeading(document, "Skills", 14, ParagraphAlignment.LEFT);
    addParagraph(document, String.join(" · ", skills), 10, false, ParagraphAlignment.LEFT);
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

  private void addHeading(XWPFDocument document, String text, int fontSize, ParagraphAlignment alignment) {
    addParagraph(document, text, fontSize, true, alignment);
  }

  private void addParagraph(
      XWPFDocument document, String text, int fontSize, boolean bold, ParagraphAlignment alignment) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setAlignment(alignment);
    XWPFRun run = paragraph.createRun();
    run.setText(text == null ? "" : text);
    run.setFontSize(fontSize);
    run.setBold(bold);
  }

  private void addBullet(XWPFDocument document, String text) {
    XWPFParagraph paragraph = document.createParagraph();
    paragraph.setIndentationLeft(360);
    XWPFRun run = paragraph.createRun();
    run.setText("• " + text);
    run.setFontSize(10);
  }
}
