package com.resumerocket.auth;

import com.resumerocket.common.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthService(
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider) {
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userAccountRepository.existsByEmail(request.email())) {
      throw ApiException.conflict("An account with this email already exists");
    }
    UserAccount user =
        new UserAccount(
            request.email(), passwordEncoder.encode(request.password()), request.fullName());
    userAccountRepository.save(user);
    return new AuthResponse(user.getId(), jwtTokenProvider.generateToken(user));
  }

  public AuthResponse login(LoginRequest request) {
    UserAccount user =
        userAccountRepository
            .findByEmail(request.email())
            .orElseThrow(() -> ApiException.badRequest("Invalid email or password"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw ApiException.badRequest("Invalid email or password");
    }
    return new AuthResponse(user.getId(), jwtTokenProvider.generateToken(user));
  }
}
