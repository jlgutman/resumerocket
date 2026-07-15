package com.resumerocket.profile;

public record ContactInfoRequest(
    String fullName, String email, String phone, String location, String links) {}
