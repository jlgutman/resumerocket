package com.resumerocket.tailoring;

/** A candidate suggestion produced by {@link AiTailoringService}, not yet persisted. */
public record SuggestionDraft(
    String targetSection, SuggestionType suggestionType, String originalText, String suggestedText) {}
