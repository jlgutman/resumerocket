package com.resumerocket.tailoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumerocket.resume.ResumeContent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Default {@link AiTailoringService} adapter: OpenAI via Spring AI's {@link ChatClient}, prompted
 * for JSON-only output (research.md §1). The model, temperature, and API key are all
 * configuration-driven (spring.ai.openai.*, backed by OPENAI_API_KEY / OPENAI_MODEL) so swapping
 * to another Spring-AI-supported provider only means changing the starter dependency and config,
 * not this class's callers.
 */
@Service
public class OpenAiTailoringServiceImpl implements AiTailoringService {

  private static final Logger log = LoggerFactory.getLogger(OpenAiTailoringServiceImpl.class);

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;

  public OpenAiTailoringServiceImpl(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = objectMapper;
  }

  @Override
  public List<String> extractRequirements(String jobDescriptionText) {
    if (jobDescriptionText == null || jobDescriptionText.isBlank()) {
      return List.of();
    }
    String systemPrompt =
        "You extract key requirements and keywords from job postings. Respond with ONLY a JSON"
            + " array of short requirement/keyword strings (skills, qualifications, tools,"
            + " responsibilities). No prose, no markdown fences.";
    String response = callModel(systemPrompt, jobDescriptionText);
    JsonNode root = parseJsonLeniently(response);
    List<String> requirements = new ArrayList<>();
    if (root != null && root.isArray()) {
      root.forEach(node -> requirements.add(node.asText()));
    }
    return requirements;
  }

  @Override
  public TailoringResult tailor(
      ResumeContent baseResume, String jobDescriptionText, List<String> extractedRequirements) {
    try {
      String resumeJson = objectMapper.writeValueAsString(baseResume);
      String requirementsJson = objectMapper.writeValueAsString(extractedRequirements);
      String systemPrompt =
          """
          You tailor resumes to job descriptions. Given a candidate's resume (JSON) and a job \
          description with extracted requirements, propose changes that emphasize relevant \
          experience, rewrite bullet points toward the job's language, and highlight relevant \
          skills. Respond with ONLY JSON matching this shape, no prose, no markdown fences:
          {
            "suggestions": [
              {
                "targetSection": "experience:<index>" | "skills" | "summary",
                "suggestionType": "EMPHASIS" | "BULLET_REWRITE" | "SKILL_HIGHLIGHT",
                "originalText": "<existing text this replaces, or null for a new skill>",
                "suggestedText": "<the proposed text>"
              }
            ],
            "unmatchedRequirements": ["<requirement with no clear match in the resume>"]
          }
          """;
      String userPrompt =
          "RESUME:\n" + resumeJson + "\n\nJOB DESCRIPTION:\n" + jobDescriptionText
              + "\n\nEXTRACTED REQUIREMENTS:\n" + requirementsJson;
      String response = callModel(systemPrompt, userPrompt);
      return parseTailoringResult(response);
    } catch (Exception ex) {
      throw new AiTailoringException("Failed to generate tailoring suggestions", ex);
    }
  }

  private TailoringResult parseTailoringResult(String response) {
    JsonNode root = parseJsonLeniently(response);
    List<SuggestionDraft> suggestions = new ArrayList<>();
    List<String> unmatched = new ArrayList<>();
    if (root != null) {
      JsonNode suggestionsNode = root.path("suggestions");
      if (suggestionsNode.isArray()) {
        suggestionsNode.forEach(
            node ->
                suggestions.add(
                    new SuggestionDraft(
                        node.path("targetSection").asText(""),
                        parseSuggestionType(node.path("suggestionType").asText("")),
                        node.path("originalText").isNull() ? null : node.path("originalText").asText(null),
                        node.path("suggestedText").asText(""))));
      }
      JsonNode unmatchedNode = root.path("unmatchedRequirements");
      if (unmatchedNode.isArray()) {
        unmatchedNode.forEach(node -> unmatched.add(node.asText()));
      }
    }
    return new TailoringResult(suggestions, unmatched);
  }

  private SuggestionType parseSuggestionType(String raw) {
    try {
      return SuggestionType.valueOf(raw);
    } catch (IllegalArgumentException ex) {
      return SuggestionType.BULLET_REWRITE;
    }
  }

  private String callModel(String systemPrompt, String userPrompt) {
    try {
      String content =
          chatClient.prompt().system(systemPrompt).user(userPrompt).call().content();
      return content == null ? "" : content;
    } catch (Exception ex) {
      log.error("OpenAI call failed", ex);
      throw new AiTailoringException("AI provider call failed", ex);
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
      log.warn("Could not parse AI response as JSON: {}", trimmed);
      return null;
    }
  }
}
