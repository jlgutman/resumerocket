package com.resumerocket.profile;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, Long> {

  List<Skill> findByMasterProfileId(Long masterProfileId);
}
