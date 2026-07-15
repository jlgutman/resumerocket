package com.resumerocket.profile;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record EducationEntryRequest(
    @NotBlank String institution,
    String credential,
    String fieldOfStudy,
    LocalDate startDate,
    LocalDate endDate,
    String description,
    int displayOrder) {}
