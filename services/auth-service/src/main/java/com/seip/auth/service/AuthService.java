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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    private final JdbcTemplate jdbcTemplate;

    private static final Map<String, String> DEFAULT_DEPARTMENT_CODES = Map.ofEntries(
            Map.entry("ENGINEERING", "ENG"),
            Map.entry("MARKETING", "MKT"),
            Map.entry("SALES", "SLS"),
            Map.entry("FINANCE", "FIN"),
            Map.entry("OPERATIONS", "OPS"),
            Map.entry("HUMAN RESOURCES", "HR"),
            Map.entry("LEGAL", "LGL"),
            Map.entry("PRODUCT", "PRD"),
            Map.entry("DESIGN", "DSN"),
            Map.entry("CUSTOMER SUPPORT", "CST")
    );

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
        String roleName = normalizeRoleName(request.getRole());

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
        synchronizeLegacyRoleColumn(savedUser.getId(), roleName);
        ensureEmployeeProfile(savedUser, request.getDepartment(), roleName);

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
        String primaryRole = extractPrimaryRole(user);
        synchronizeLegacyRoleColumn(user.getId(), primaryRole);
        ensureEmployeeProfile(user, null, primaryRole);
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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcileLegacyRoleColumn() {
        int updated = jdbcTemplate.update("""
                UPDATE auth.users u
                SET role = roles.role_name
                FROM (
                    SELECT ur.user_id, MAX(r.name) AS role_name
                    FROM auth.user_roles ur
                    JOIN auth.roles r ON r.id = ur.role_id
                    GROUP BY ur.user_id
                ) roles
                WHERE u.id = roles.user_id
                  AND COALESCE(u.role, '') <> roles.role_name
                """);

        if (updated > 0) {
            log.info("Reconciled legacy auth.users.role values for {} user(s)", updated);
        }
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

    private String normalizeRoleName(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return "ROLE_EMPLOYEE";
        }

        String normalized = requestedRole.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private String extractPrimaryRole(User user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_EMPLOYEE");
    }

    private void synchronizeLegacyRoleColumn(Long userId, String roleName) {
        jdbcTemplate.update("""
                UPDATE auth.users
                SET role = ?
                WHERE id = ?
                  AND COALESCE(role, '') <> ?
                """, roleName, userId, roleName);
    }

    private void ensureEmployeeProfile(User user, String requestedDepartment, String roleName) {
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users.employees WHERE auth_user_id = ?",
                Integer.class,
                user.getId()
        );

        Long departmentId = resolveDepartmentId(requestedDepartment);
        String designation = "ROLE_MANAGER".equals(roleName) ? "Manager" : "Employee";

        if (existing != null && existing > 0) {
            if (departmentId != null) {
                jdbcTemplate.update("""
                        UPDATE users.employees
                        SET email = ?,
                            designation = COALESCE(designation, ?),
                            department_id = COALESCE(department_id, ?),
                            updated_at = CURRENT_TIMESTAMP
                        WHERE auth_user_id = ?
                        """,
                        user.getEmail(),
                        designation,
                        departmentId,
                        user.getId()
                );
            }
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO users.employees (
                    auth_user_id,
                    employee_code,
                    first_name,
                    last_name,
                    email,
                    designation,
                    monthly_expense_limit,
                    is_active,
                    join_date,
                    department_id,
                    created_at,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                user.getId(),
                generateEmployeeCode(user.getId()),
                user.getUsername(),
                "User",
                user.getEmail(),
                designation,
                50000,
                true,
                departmentId
        );
    }

    private Long resolveDepartmentId(String requestedDepartment) {
        if (requestedDepartment == null || requestedDepartment.isBlank()) {
            return null;
        }

        String normalizedName = requestedDepartment.trim();
        List<Long> ids = jdbcTemplate.query(
                "SELECT id FROM users.departments WHERE LOWER(name) = LOWER(?) LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"),
                normalizedName
        );

        if (!ids.isEmpty()) {
            return ids.get(0);
        }

        String code = Optional.ofNullable(DEFAULT_DEPARTMENT_CODES.get(normalizedName.toUpperCase(Locale.ROOT)))
                .orElseGet(() -> generateDepartmentCode(normalizedName));

        return jdbcTemplate.queryForObject("""
                INSERT INTO users.departments (name, code, budget, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (code) DO UPDATE
                SET name = EXCLUDED.name,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING id
                """,
                Long.class,
                normalizedName,
                code,
                0
        );
    }

    private String generateDepartmentCode(String departmentName) {
        String[] parts = departmentName.trim().toUpperCase(Locale.ROOT).split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                builder.append(part.charAt(0));
            }
        }

        String code = builder.length() > 0 ? builder.toString() : departmentName.replaceAll("[^A-Za-z]", "")
                .toUpperCase(Locale.ROOT);
        return code.length() > 6 ? code.substring(0, 6) : code;
    }

    private String generateEmployeeCode(Long authUserId) {
        return String.format("EMP-%05d", authUserId);
    }
}
