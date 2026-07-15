package com.resumerocket.resume;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.resumerocket.resume.DiffLine.DiffLineType;
import com.resumerocket.resume.ResumeContent.ExperienceItem;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Section-by-section diff between two tailored resume versions (FR-017, research.md §5). */
@Service
public class ResumeDiffService {

  public List<SectionDiff> compare(ResumeContent left, ResumeContent right) {
    List<SectionDiff> sections = new ArrayList<>();
    sections.add(new SectionDiff("summary", diffLines(List.of(nullToEmpty(left.summary())), List.of(nullToEmpty(right.summary())))));
    sections.add(new SectionDiff("experience", diffLines(experienceLines(left), experienceLines(right))));
    sections.add(new SectionDiff("education", diffLines(educationLines(left), educationLines(right))));
    sections.add(new SectionDiff("skills", diffLines(left.skills(), right.skills())));
    return sections;
  }

  private List<String> experienceLines(ResumeContent content) {
    List<String> lines = new ArrayList<>();
    for (ExperienceItem item : content.experience()) {
      lines.add(item.title() + " — " + item.company());
      for (String bullet : item.bullets()) {
        lines.add("  • " + bullet);
      }
    }
    return lines;
  }

  private List<String> educationLines(ResumeContent content) {
    List<String> lines = new ArrayList<>();
    content
        .education()
        .forEach(
            item ->
                lines.add(
                    (item.credential() != null ? item.credential() + ", " : "") + item.institution()));
    return lines;
  }

  private List<DiffLine> diffLines(List<String> original, List<String> revised) {
    Patch<String> patch = DiffUtils.diff(original, revised);
    List<DiffLine> result = new ArrayList<>();
    int cursor = 0;
    for (AbstractDelta<String> delta : patch.getDeltas()) {
      int position = delta.getSource().getPosition();
      for (int i = cursor; i < position; i++) {
        result.add(new DiffLine(DiffLineType.UNCHANGED, original.get(i)));
      }
      delta.getSource().getLines().forEach(line -> result.add(new DiffLine(DiffLineType.REMOVED, line)));
      delta.getTarget().getLines().forEach(line -> result.add(new DiffLine(DiffLineType.ADDED, line)));
      cursor = position + delta.getSource().size();
    }
    for (int i = cursor; i < original.size(); i++) {
      result.add(new DiffLine(DiffLineType.UNCHANGED, original.get(i)));
    }
    return result;
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
