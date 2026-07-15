package com.resumerocket.export;

import com.resumerocket.resume.ResumeContent;
import com.resumerocket.resume.ResumeContent.EducationItem;
import com.resumerocket.resume.ResumeContent.ExperienceItem;
import com.resumerocket.template.LayoutDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Plain, unstyled text with no tables/columns/images so ATS parsers can read it (FR-013). */
@Service
public class PlaintextExportService implements ResumeExporter {

  @Override
  public ExportFormat format() {
    return ExportFormat.txt;
  }

  @Override
  public String contentType() {
    return "text/plain; charset=utf-8";
  }

  @Override
  public byte[] export(ResumeContent content, LayoutDescriptor layoutDescriptor) {
    StringBuilder sb = new StringBuilder();
    var contact = content.contactInfo();
    sb.append(nullToEmpty(contact.fullName())).append('\n');
    List<String> contactParts = new ArrayList<>();
    if (contact.email() != null) contactParts.add(contact.email());
    if (contact.phone() != null) contactParts.add(contact.phone());
    if (contact.location() != null) contactParts.add(contact.location());
    if (contact.links() != null) contactParts.add(contact.links());
    sb.append(String.join(" | ", contactParts)).append("\n\n");

    for (String section : layoutDescriptor.sectionOrder()) {
      switch (section) {
        case "summary" -> writeSummary(sb, content);
        case "experience" -> writeExperience(sb, content.experience());
        case "education" -> writeEducation(sb, content.education());
        case "skills" -> writeSkills(sb, content.skills());
        default -> {
          // Unknown section names are ignored so new templates degrade gracefully.
        }
      }
    }

    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private void writeSummary(StringBuilder sb, ResumeContent content) {
    if (content.summary() == null || content.summary().isBlank()) {
      return;
    }
    sb.append("SUMMARY\n").append(content.summary()).append("\n\n");
  }

  private void writeExperience(StringBuilder sb, List<ExperienceItem> experience) {
    if (experience.isEmpty()) {
      return;
    }
    sb.append("EXPERIENCE\n");
    for (ExperienceItem item : experience) {
      sb.append(item.title()).append(" - ").append(item.company()).append('\n');
      sb.append(item.startDate())
          .append(" - ")
          .append(item.currentRole() ? "Present" : item.endDate())
          .append('\n');
      for (String bullet : item.bullets()) {
        sb.append("- ").append(bullet).append('\n');
      }
      sb.append('\n');
    }
  }

  private void writeEducation(StringBuilder sb, List<EducationItem> education) {
    if (education.isEmpty()) {
      return;
    }
    sb.append("EDUCATION\n");
    for (EducationItem item : education) {
      if (item.credential() != null) {
        sb.append(item.credential()).append(", ");
      }
      sb.append(item.institution()).append('\n');
      if (item.fieldOfStudy() != null && !item.fieldOfStudy().isBlank()) {
        sb.append(item.fieldOfStudy()).append('\n');
      }
      sb.append('\n');
    }
  }

  private void writeSkills(StringBuilder sb, List<String> skills) {
    if (skills.isEmpty()) {
      return;
    }
    sb.append("SKILLS\n").append(String.join(", ", skills)).append("\n\n");
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
