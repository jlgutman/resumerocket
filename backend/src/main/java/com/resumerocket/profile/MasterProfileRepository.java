package com.resumerocket.profile;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterProfileRepository extends JpaRepository<MasterProfile, Long> {

  Optional<MasterProfile> findByUserAccountId(Long userAccountId);
}
