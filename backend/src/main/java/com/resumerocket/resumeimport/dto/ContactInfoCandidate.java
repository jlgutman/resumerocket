package com.resumerocket.resumeimport.dto;

import java.util.List;

public record ContactInfoCandidate(
    String fullName,
    String email,
    String phone,
    String location,
    String links,
    List<String> fieldsNotExtracted) {}
