package com.resumerocket.jobdescription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumerocket.common.ApiException;
import com.resumerocket.tailoring.AiTailoringException;
import com.resumerocket.tailoring.AiTailoringService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobDescriptionService {

  private static final Logger log = LoggerFactory.getLogger(JobDescriptionService.class);

  private final JobDescriptionRepository jobDescriptionRepository;
  private final AiTailoringService aiTailoringService;
  private final ObjectMapper objectMapper;

  public JobDescriptionService(
      JobDescriptionRepository jobDescriptionRepository,
      AiTailoringService aiTailoringService,
      ObjectMapper objectMapper) {
    this.jobDescriptionRepository = jobDescriptionRepository;
    this.aiTailoringService = aiTailoringService;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public JobDescriptionResponse submit(Long userAccountId, JobDescriptionRequest request) {
    if (request.rawText() == null || request.rawText().isBlank()) {
      throw ApiException.badRequest("Job description text must not be empty");
    }
    JobDescription jobDescription = new JobDescription(userAccountId, request.rawText());
    List<String> requirements;
    try {
      requirements = aiTailoringService.extractRequirements(request.rawText());
    } catch (AiTailoringException ex) {
      log.warn("Requirement extraction failed, continuing with an empty set: {}", ex.getMessage());
      requirements = List.of();
    }
    jobDescription.setExtractedRequirementsJson(toJson(requirements));
    jobDescriptionRepository.save(jobDescription);
    return new JobDescriptionResponse(jobDescription.getId(), requirements);
  }

  public JobDescription requireOwned(Long userAccountId, Long id) {
    JobDescription jobDescription =
        jobDescriptionRepository
            .findById(id)
            .orElseThrow(() -> ApiException.notFound("Job description not found"));
    if (!jobDescription.getUserAccountId().equals(userAccountId)) {
      throw ApiException.forbidden("This job description does not belong to the current user");
    }
    return jobDescription;
  }

  public List<String> readRequirements(JobDescription jobDescription) {
    return fromJson(jobDescription.getExtractedRequirementsJson());
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to serialize job description data", ex);
    }
  }

  private List<String> fromJson(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (Exception ex) {
      return List.of();
    }
  }
}
