package com.resumerocket.template;

import com.resumerocket.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "template")
public class Template extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Lob
  @Column(name = "layout_descriptor", nullable = false, length = 16777216)
  private String layoutDescriptorJson;

  protected Template() {}

  public String getName() {
    return name;
  }

  public String getLayoutDescriptorJson() {
    return layoutDescriptorJson;
  }
}
