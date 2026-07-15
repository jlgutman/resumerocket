package com.resumerocket.profile;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkExperienceEntryRepository extends JpaRepository<WorkExperienceEntry, Long> {

  List<WorkExperienceEntry> findByMasterProfileIdOrderByDisplayOrderAsc(Long masterProfileId);
}
