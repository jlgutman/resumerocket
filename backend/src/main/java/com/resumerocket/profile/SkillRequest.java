package com.resumerocket.profile;

import jakarta.validation.constraints.NotBlank;

public record SkillRequest(@NotBlank String name, String category) {}
