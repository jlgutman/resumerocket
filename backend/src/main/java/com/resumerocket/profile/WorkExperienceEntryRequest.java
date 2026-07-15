package com.resumerocket.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record WorkExperienceEntryRequest(
    @NotBlank String company,
    @NotBlank String title,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @NotBlank String description,
    int displayOrder) {}
