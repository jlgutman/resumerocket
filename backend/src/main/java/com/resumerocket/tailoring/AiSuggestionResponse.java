package com.resumerocket.tailoring;

public record AiSuggestionResponse(
    Long id,
    String targetSection,
    SuggestionType suggestionType,
    String originalText,
    String suggestedText,
    String finalText,
    ReviewState reviewState) {

  public static AiSuggestionResponse from(AiSuggestion suggestion) {
    return new AiSuggestionResponse(
        suggestion.getId(),
        suggestion.getTargetSection(),
        suggestion.getSuggestionType(),
        suggestion.getOriginalText(),
        suggestion.getSuggestedText(),
        suggestion.getFinalText(),
        suggestion.getReviewState());
  }
}
