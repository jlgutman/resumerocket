package com.resumerocket.profile;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationEntryRepository extends JpaRepository<EducationEntry, Long> {

  List<EducationEntry> findByMasterProfileIdOrderByDisplayOrderAsc(Long masterProfileId);
}
