package com.resumerocket.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reads the {@link AuthenticatedUser} that {@link JwtAuthFilter} placed on the security context. */
@Component
public class CurrentUserProvider {

  public AuthenticatedUser require() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (!(principal instanceof AuthenticatedUser user)) {
      throw new IllegalStateException("No authenticated user in security context");
    }
    return user;
  }
}
