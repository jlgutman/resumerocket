package com.resumerocket.tailoring;

import jakarta.validation.constraints.NotNull;

public record SuggestionResolutionRequest(@NotNull ReviewState reviewState, String finalText) {}
