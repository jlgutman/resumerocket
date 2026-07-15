package com.resumerocket.template;

import java.util.List;

/**
 * Structured layout/style definition consumed by both the frontend preview renderer and the
 * backend exporters (research.md §4), so a template only needs to be defined once.
 */
public record LayoutDescriptor(
    String style, String accentColor, String fontFamily, List<String> sectionOrder) {}
