package com.resumerocket.jobdescription;

import java.util.List;

public record JobDescriptionResponse(Long id, List<String> extractedRequirements) {}
