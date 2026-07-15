package com.resumerocket.tailoring;

import java.util.List;

public record TailoringResult(List<SuggestionDraft> suggestions, List<String> unmatchedRequirements) {}
