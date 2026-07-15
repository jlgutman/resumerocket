package com.resumerocket.jobdescription;

import jakarta.validation.constraints.NotBlank;

public record JobDescriptionRequest(@NotBlank String rawText) {}
