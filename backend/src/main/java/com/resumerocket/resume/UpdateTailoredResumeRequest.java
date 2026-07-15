package com.resumerocket.resume;

public record UpdateTailoredResumeRequest(
    String name, String company, String jobTitle, ResumeStatus status) {}
