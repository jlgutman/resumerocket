package com.resumerocket.auth;

/** Principal stored in the security context for a JWT-authenticated request. */
public record AuthenticatedUser(Long id, String email) {}
