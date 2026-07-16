package com.resumerocket.resumeimport.dto;

import java.time.LocalDate;

public record EducationCandidate(
    String institution,
    String credential,
    String fieldOfStudy,
    LocalDate startDate,
    LocalDate endDate,
    String description,
    boolean lowConfidence) {}
