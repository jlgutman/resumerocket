package com.resumerocket.resume;

import com.resumerocket.tailoring.AiSuggestionResponse;
import java.time.Instant;
import java.util.List;

public record TailoredResumeResponse(
    Long id,
    Long jobDescriptionId,
    String name,
    String company,
    String jobTitle,
    ResumeStatus status,
    ResumeContent content,
    List<AiSuggestionResponse> suggestions,
    List<String> unmatchedRequirements,
    Instant createdAt,
    Long clonedFromId,
    Long regeneratedFromId) {}
