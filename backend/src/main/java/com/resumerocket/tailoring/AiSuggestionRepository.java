package com.resumerocket.tailoring;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, Long> {

  List<AiSuggestion> findByTailoredResumeId(Long tailoredResumeId);
}
