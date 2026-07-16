package com.resumerocket.resumeimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumerocket.common.ApiException;
import com.resumerocket.resumeimport.dto.ContactInfoCandidate;
import com.resumerocket.resumeimport.dto.EducationCandidate;
import com.resumerocket.resumeimport.dto.ResumeImportResult;
import com.resumerocket.resumeimport.dto.SkillCandidate;
import com.resumerocket.resumeimport.dto.WorkExperienceCandidate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Default {@link AiResumeExtractionService} adapter: OpenAI via Spring AI's {@link ChatClient},
 * prompted for JSON-only output (research.md #2), mirroring {@code
 * tailoring.OpenAiTailoringServiceImpl}'s call and lenient-parsing pattern.
 */
@Service
public class OpenAiResumeExtractionServiceImpl implements AiResumeExtractionService {

  private static final Logger log = LoggerFactory.getLogger(OpenAiResumeExtractionServiceImpl.class);

  private static final String SYSTEM_PROMPT =
      """
      You extract structured profile data from resume text. Respond with ONLY JSON matching this \
      shape, no prose, no markdown fences:
      {
        "contactInfo": {
          "fullName": "<string or null>",
          "email": "<string or null>",
          "phone": "<string or null>",
          "location": "<string or null>",
          "links": "<comma-separated links, or null>",
          "fieldsNotExtracted": ["<name of any contactInfo field above you could not confidently fill, e.g. \\"phone\\">"]
        },
        "workExperience": [
          {
            "company": "<string>",
            "title": "<string>",
            "startDate": "<YYYY-MM-DD or null if unknown>",
            "endDate": "<YYYY-MM-DD, or null if this is the current/most recent role>",
            "description": "<summary of responsibilities/achievements>",
            "lowConfidence": <true if any field on this entry could not be confidently extracted>
          }
        ],
        "education": [
          {
            "institution": "<string>",
            "credential": "<degree/certificate, or null>",
            "fieldOfStudy": "<string or null>",
            "startDate": "<YYYY-MM-DD or null>",
            "endDate": "<YYYY-MM-DD, or null if in progress>",
            "description": "<string or null>",
            "lowConfidence": <true if any field on this entry could not be confidently extracted>
          }
        ],
        "skills": [
          { "name": "<string>", "category": "<string or null>", "lowConfidence": <boolean> }
        ],
        "warnings": ["<short human-readable note about anything unusual, e.g. multi-column layout>"]
      }

      Only mark a field confidently extracted if the resume text actually supports it. Never \
      invent or guess a value — if a field isn't clearly present, set it to null (or omit the \
      entry's non-required fields) and list it in fieldsNotExtracted / set lowConfidence true \
      instead of fabricating a plausible-looking value. Dates must be ISO-8601 (YYYY-MM-DD); if \
      only a year or month is known, use the first day of that period.
      """;

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;

  public OpenAiResumeExtractionServiceImpl(
      ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = objectMapper;
  }

  @Override
  public ResumeImportResult extract(String resumeText) {
    String response = callModel(resumeText);
    JsonNode root = parseJsonLeniently(response);
    if (root == null) {
      throw extractionFailed();
    }
    return new ResumeImportResult(
        null,
        parseContactInfo(root.path("contactInfo")),
        parseWorkExperience(root.path("workExperience")),
        parseEducation(root.path("education")),
        parseSkills(root.path("skills")),
        parseStringList(root.path("warnings")));
  }

  private String callModel(String resumeText) {
    try {
      String content = chatClient.prompt().system(SYSTEM_PROMPT).user(resumeText).call().content();
      return content == null ? "" : content;
    } catch (Exception ex) {
      log.error("OpenAI resume extraction call failed", ex);
      throw extractionFailed();
    }
  }

  private ApiException extractionFailed() {
    return new ApiException(
        HttpStatus.BAD_GATEWAY,
        "We couldn't process this resume right now. Please try again in a moment, or enter your"
            + " details manually.");
  }

  private ContactInfoCandidate parseContactInfo(JsonNode node) {
    return new ContactInfoCandidate(
        textOrNull(node, "fullName"),
        textOrNull(node, "email"),
        textOrNull(node, "phone"),
        textOrNull(node, "location"),
        textOrNull(node, "links"),
        parseStringList(node.path("fieldsNotExtracted")));
  }

  private List<WorkExperienceCandidate> parseWorkExperience(JsonNode arrayNode) {
    List<WorkExperienceCandidate> result = new ArrayList<>();
    if (arrayNode.isArray()) {
      arrayNode.forEach(
          node ->
              result.add(
                  new WorkExperienceCandidate(
                      node.path("company").asText(""),
                      node.path("title").asText(""),
                      dateOrNull(node, "startDate"),
                      dateOrNull(node, "endDate"),
                      node.path("description").asText(""),
                      node.path("lowConfidence").asBoolean(false))));
    }
    return result;
  }

  private List<EducationCandidate> parseEducation(JsonNode arrayNode) {
    List<EducationCandidate> result = new ArrayList<>();
    if (arrayNode.isArray()) {
      arrayNode.forEach(
          node ->
              result.add(
                  new EducationCandidate(
                      node.path("institution").asText(""),
                      textOrNull(node, "credential"),
                      textOrNull(node, "fieldOfStudy"),
                      dateOrNull(node, "startDate"),
                      dateOrNull(node, "endDate"),
                      textOrNull(node, "description"),
                      node.path("lowConfidence").asBoolean(false))));
    }
    return result;
  }

  private List<SkillCandidate> parseSkills(JsonNode arrayNode) {
    List<SkillCandidate> result = new ArrayList<>();
    if (arrayNode.isArray()) {
      arrayNode.forEach(
          node ->
              result.add(
                  new SkillCandidate(
                      node.path("name").asText(""),
                      textOrNull(node, "category"),
                      node.path("lowConfidence").asBoolean(false))));
    }
    return result;
  }

  private List<String> parseStringList(JsonNode arrayNode) {
    List<String> result = new ArrayList<>();
    if (arrayNode.isArray()) {
      arrayNode.forEach(node -> result.add(node.asText()));
    }
    return result;
  }

  private String textOrNull(JsonNode parent, String field) {
    JsonNode node = parent.path(field);
    return node.isMissingNode() || node.isNull() ? null : node.asText(null);
  }

  private LocalDate dateOrNull(JsonNode parent, String field) {
    String raw = textOrNull(parent, field);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(raw);
    } catch (Exception ex) {
      return null;
    }
  }

  private JsonNode parseJsonLeniently(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String trimmed = text.trim();
    // Models sometimes wrap JSON in ```json fences despite instructions not to.
    if (trimmed.startsWith("```")) {
      trimmed = trimmed.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
    }
    try {
      return objectMapper.readTree(trimmed);
    } catch (Exception ex) {
      log.warn("Could not parse AI resume-extraction response as JSON: {}", trimmed);
      return null;
    }
  }
}
