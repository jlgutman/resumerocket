package com.resumerocket.tailoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumerocket.common.ApiException;
import com.resumerocket.jobdescription.JobDescription;
import com.resumerocket.jobdescription.JobDescriptionService;
import com.resumerocket.profile.EducationEntryResponse;
import com.resumerocket.profile.ProfileResponse;
import com.resumerocket.profile.ProfileService;
import com.resumerocket.profile.WorkExperienceEntryResponse;
import com.resumerocket.resume.ResumeContent;
import com.resumerocket.resume.ResumeContent.ContactInfoSnapshot;
import com.resumerocket.resume.ResumeContent.EducationItem;
import com.resumerocket.resume.ResumeContent.ExperienceItem;
import com.resumerocket.resume.TailoredResume;
import com.resumerocket.resume.TailoredResumeRepository;
import com.resumerocket.resume.TailoredResumeResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TailoringService {

  private static final Logger log = LoggerFactory.getLogger(TailoringService.class);
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

  private final ProfileService profileService;
  private final JobDescriptionService jobDescriptionService;
  private final AiTailoringService aiTailoringService;
  private final TailoredResumeRepository tailoredResumeRepository;
  private final AiSuggestionRepository aiSuggestionRepository;
  private final ObjectMapper objectMapper;

  public TailoringService(
      ProfileService profileService,
      JobDescriptionService jobDescriptionService,
      AiTailoringService aiTailoringService,
      TailoredResumeRepository tailoredResumeRepository,
      AiSuggestionRepository aiSuggestionRepository,
      ObjectMapper objectMapper) {
    this.profileService = profileService;
    this.jobDescriptionService = jobDescriptionService;
    this.aiTailoringService = aiTailoringService;
    this.tailoredResumeRepository = tailoredResumeRepository;
    this.aiSuggestionRepository = aiSuggestionRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public TailoredResumeResponse generateDraft(Long userAccountId, Long jobDescriptionId) {
    JobDescription jobDescription = jobDescriptionService.requireOwned(userAccountId, jobDescriptionId);
    ResumeContent baseResume = buildResumeContentFromProfile(profileService.getProfileView(userAccountId));
    List<String> requirements = jobDescriptionService.readRequirements(jobDescription);

    TailoringResult result;
    try {
      result = aiTailoringService.tailor(baseResume, jobDescription.getRawText(), requirements);
    } catch (AiTailoringException ex) {
      log.warn("AI tailoring failed, returning an untailored draft: {}", ex.getMessage());
      result = new TailoringResult(List.of(), requirements);
    }

    String resumeJson = writeJson(baseResume);
    TailoredResume resume =
        new TailoredResume(
            userAccountId,
            jobDescriptionId,
            resumeJson,
            resumeJson,
            defaultName(jobDescription));
    resume.setUnmatchedRequirementsJson(writeJson(result.unmatchedRequirements()));
    tailoredResumeRepository.save(resume);

    for (SuggestionDraft draft : result.suggestions()) {
      aiSuggestionRepository.save(
          new AiSuggestion(
              resume.getId(),
              draft.targetSection(),
              draft.suggestionType(),
              draft.originalText(),
              draft.suggestedText()));
    }

    return toResponse(resume);
  }

  public TailoredResumeResponse toResponse(TailoredResume resume) {
    List<AiSuggestionResponse> suggestions =
        aiSuggestionRepository.findByTailoredResumeId(resume.getId()).stream()
            .map(AiSuggestionResponse::from)
            .toList();
    return new TailoredResumeResponse(
        resume.getId(),
        resume.getJobDescriptionId(),
        resume.getName(),
        resume.getCompany(),
        resume.getJobTitle(),
        resume.getStatus(),
        readResumeContent(resume),
        suggestions,
        readUnmatchedRequirements(resume),
        resume.getCreatedAt(),
        resume.getClonedFromId(),
        resume.getRegeneratedFromId());
  }

  public ResumeContent readResumeContent(TailoredResume resume) {
    try {
      return objectMapper.readValue(resume.getResumeContentJson(), ResumeContent.class);
    } catch (Exception ex) {
      throw new IllegalStateException("Corrupt resume content for resume " + resume.getId(), ex);
    }
  }

  public String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to serialize resume data", ex);
    }
  }

  private List<String> readUnmatchedRequirements(TailoredResume resume) {
    String json = resume.getUnmatchedRequirementsJson();
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(
          json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (Exception ex) {
      return List.of();
    }
  }

  private String defaultName(JobDescription jobDescription) {
    String snippet = jobDescription.getRawText().strip();
    if (snippet.length() > 40) {
      snippet = snippet.substring(0, 40) + "...";
    }
    return snippet.isBlank() ? "Untitled tailored resume" : snippet;
  }

  ResumeContent buildResumeContentFromProfile(ProfileResponse profile) {
    ContactInfoSnapshot contactInfo =
        new ContactInfoSnapshot(
            profile.fullName(), profile.email(), profile.phone(), profile.location(), profile.links());

    List<ExperienceItem> experience =
        profile.workExperienceEntries().stream()
            .map(this::toExperienceItem)
            .toList();

    List<EducationItem> education =
        profile.educationEntries().stream().map(this::toEducationItem).toList();

    List<String> skills = profile.skills().stream().map(s -> s.name()).toList();

    return new ResumeContent(contactInfo, "", experience, education, skills);
  }

  private ExperienceItem toExperienceItem(WorkExperienceEntryResponse entry) {
    return new ExperienceItem(
        entry.company(),
        entry.title(),
        formatDate(entry.startDate()),
        entry.currentRole() ? null : formatDate(entry.endDate()),
        entry.currentRole(),
        List.of(entry.description()));
  }

  private EducationItem toEducationItem(EducationEntryResponse entry) {
    return new EducationItem(
        entry.institution(),
        entry.credential(),
        entry.fieldOfStudy(),
        formatDate(entry.startDate()),
        formatDate(entry.endDate()),
        entry.description());
  }

  private String formatDate(java.time.LocalDate date) {
    return date == null ? null : date.format(DATE_FORMAT);
  }

  public TailoredResume requireOwned(Long userAccountId, Long id) {
    return tailoredResumeRepository
        .findByIdAndUserAccountId(id, userAccountId)
        .orElseThrow(() -> ApiException.notFound("Tailored resume not found"));
  }
}
