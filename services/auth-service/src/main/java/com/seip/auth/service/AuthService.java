package com.seip.auth.service;

import com.seip.auth.dto.AuthResponse;
import com.seip.auth.dto.LoginRequest;
import com.seip.auth.dto.RegisterRequest;
import com.seip.auth.dto.UserInfoResponse;
import com.seip.auth.entity.RefreshToken;
import com.seip.auth.entity.Role;
import com.seip.auth.entity.User;
import com.seip.auth.exception.ResourceNotFoundException;
import com.seip.auth.exception.UserAlreadyExistsException;
import com.seip.auth.repository.RefreshTokenRepository;
import com.seip.auth.repository.RoleRepository;
import com.seip.auth.repository.UserRepository;
import com.seip.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(
                    "Username '" + request.getUsername() + "' is already taken.");
        }
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Email '" + request.getEmail() + "' is already registered.");
        }

        // Resolve role — default to ROLE_EMPLOYEE if not valid
        String roleName = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole()
                : "ROLE_EMPLOYEE";

        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.findByName("ROLE_EMPLOYEE")
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Role", "name", "ROLE_EMPLOYEE")));

        // Build and persist user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        user.getRoles().add(role);
        User savedUser = userRepository.save(user);

        log.info("Registered new user: {} with role: {}", savedUser.getUsername(), roleName);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        return buildAuthResponse(savedUser, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Delegate credential verification to AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        User user = (User) authentication.getPrincipal();
        log.info("User logged in: {}", user.getUsername());

        // Revoke any existing refresh tokens before issuing a new one
        refreshTokenService.revokeAllUserTokens(user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse refreshToken(String tokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RefreshToken", "token", tokenValue));

        // Verify not expired or revoked — throws if invalid
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);

        log.info("Issued new access token for user: {}", user.getUsername());

        return buildAuthResponse(user, newAccessToken, refreshToken.getToken());
    }

    @Transactional
    public void logout(String tokenValue) {
        refreshTokenService.revokeToken(tokenValue);
        log.info("User logged out via refresh token revocation");
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        String primaryRole = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_EMPLOYEE");

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiryMs())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(primaryRole)
                .build();
    }
}
