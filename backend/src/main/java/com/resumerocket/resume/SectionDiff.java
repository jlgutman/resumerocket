package com.resumerocket.resume;

import java.util.List;

public record SectionDiff(String section, List<DiffLine> lines) {}
