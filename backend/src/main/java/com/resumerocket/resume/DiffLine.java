package com.resumerocket.resume;

public record DiffLine(DiffLineType type, String text) {

  public enum DiffLineType {
    UNCHANGED,
    ADDED,
    REMOVED
  }
}
