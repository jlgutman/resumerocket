package com.resumerocket.profile;

import java.util.List;

public record ProfileResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    String location,
    String links,
    List<EducationEntryResponse> educationEntries,
    List<WorkExperienceEntryResponse> workExperienceEntries,
    List<SkillResponse> skills) {}
