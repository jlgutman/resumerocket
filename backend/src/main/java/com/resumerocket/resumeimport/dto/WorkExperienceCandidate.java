package com.resumerocket.resumeimport.dto;

import java.time.LocalDate;

public record WorkExperienceCandidate(
    String company,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    String description,
    boolean lowConfidence) {}
