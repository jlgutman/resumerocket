package com.resumerocket.profile;

import com.resumerocket.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "skill")
public class Skill extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "master_profile_id", nullable = false)
  private MasterProfile masterProfile;

  @Column(nullable = false)
  private String name;

  @Column(length = 100)
  private String category;

  protected Skill() {}

  public Skill(MasterProfile masterProfile, String name, String category) {
    this.masterProfile = masterProfile;
    this.name = name;
    this.category = category;
  }

  public MasterProfile getMasterProfile() {
    return masterProfile;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }
}
