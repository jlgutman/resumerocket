package com.resumerocket.profile;

import com.resumerocket.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "work_experience_entry")
public class WorkExperienceEntry extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "master_profile_id", nullable = false)
  private MasterProfile masterProfile;

  @Column(nullable = false)
  private String company;

  @Column(nullable = false)
  private String title;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  /** Null end date means the role is current/ongoing. */
  @Column(name = "end_date")
  private LocalDate endDate;

  @Lob
  @Column(nullable = false, length = 65535)
  private String description;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  protected WorkExperienceEntry() {}

  public WorkExperienceEntry(MasterProfile masterProfile) {
    this.masterProfile = masterProfile;
  }

  public MasterProfile getMasterProfile() {
    return masterProfile;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public boolean isCurrentRole() {
    return endDate == null;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
