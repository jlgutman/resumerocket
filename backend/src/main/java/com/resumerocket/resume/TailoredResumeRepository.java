package com.resumerocket.resume;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TailoredResumeRepository extends JpaRepository<TailoredResume, Long> {

  List<TailoredResume> findByUserAccountIdOrderByCreatedAtDesc(Long userAccountId);

  List<TailoredResume> findByUserAccountIdAndCompanyContainingIgnoreCaseOrderByCreatedAtDesc(
      Long userAccountId, String company);

  Optional<TailoredResume> findByIdAndUserAccountId(Long id, Long userAccountId);
}
