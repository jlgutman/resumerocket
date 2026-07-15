package com.resumerocket.template;

import com.resumerocket.resume.ResumeContent;

public record RenderedLayout(ResumeContent content, LayoutDescriptor layoutDescriptor) {}
