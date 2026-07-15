package com.resumerocket.profile;

import com.resumerocket.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "master_profile")
public class MasterProfile extends BaseEntity {

  @Column(name = "user_account_id", nullable = false, unique = true)
  private Long userAccountId;

  @Column(name = "full_name")
  private String fullName;

  private String email;

  @Column(length = 50)
  private String phone;

  private String location;

  /** Comma-separated list of links (portfolio, LinkedIn, GitHub, etc.). */
  @Column(length = 1000)
  private String links;

  protected MasterProfile() {}

  public MasterProfile(Long userAccountId) {
    this.userAccountId = userAccountId;
  }

  public Long getUserAccountId() {
    return userAccountId;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getLinks() {
    return links;
  }

  public void setLinks(String links) {
    this.links = links;
  }
}
