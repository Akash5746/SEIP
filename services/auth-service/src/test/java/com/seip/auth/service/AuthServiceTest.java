package com.seip.auth.service;

import com.seip.auth.dto.AuthResponse;
import com.seip.auth.dto.LoginRequest;
import com.seip.auth.dto.RegisterRequest;
import com.seip.auth.entity.RefreshToken;
import com.seip.auth.entity.Role;
import com.seip.auth.entity.User;
import com.seip.auth.exception.TokenExpiredException;
import com.seip.auth.exception.UserAlreadyExistsException;
import com.seip.auth.repository.RefreshTokenRepository;
import com.seip.auth.repository.RoleRepository;
import com.seip.auth.repository.UserRepository;
import com.seip.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Role employeeRole;
    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        employeeRole = Role.builder()
                .id(1L)
                .name("ROLE_EMPLOYEE")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(employeeRole);

        testUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .password("$2a$12$encodedPassword")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(roles)
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("uuid-refresh-token-value")
                .user(testUser)
                .expiryDate(Instant.now().plusSeconds(604800))
                .revoked(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register() - success: user saved and tokens generated")
    void testRegisterSuccess() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username("johndoe")
                .email("john@example.com")
                .password("password123")
                .role("ROLE_EMPLOYEE")
                .build();

        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("ROLE_EMPLOYEE")).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token-value");
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(testRefreshToken);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-value");
        assertThat(response.getRefreshToken()).isEqualTo("uuid-refresh-token-value");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("johndoe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getExpiresIn()).isEqualTo(900000L);

        // Verify user was saved with encoded password
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$2a$12$encodedPassword");

        verify(jwtService).generateAccessToken(any(User.class));
        verify(refreshTokenService).createRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("register() - throws UserAlreadyExistsException when username is taken")
    void testRegisterDuplicateUsernameThrowsException() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username("johndoe")
                .email("other@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("johndoe");

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("register() - throws UserAlreadyExistsException when email is already registered")
    void testRegisterDuplicateEmailThrowsException() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("john@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("john@example.com");

        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() - success: authenticate called and tokens returned")
    void testLoginSuccess() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("johndoe")
                .password("password123")
                .build();

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        doNothing().when(refreshTokenService).revokeAllUserTokens(testUser.getId());
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token-value");
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(refreshTokenService.createRefreshToken(testUser)).thenReturn(testRefreshToken);

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token-value");
        assertThat(response.getRefreshToken()).isEqualTo("uuid-refresh-token-value");
        assertThat(response.getUserId()).isEqualTo(1L);

        // Verify authenticate was called with correct credentials
        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertThat(authCaptor.getValue().getPrincipal()).isEqualTo("johndoe");
        assertThat(authCaptor.getValue().getCredentials()).isEqualTo("password123");

        // Verify old tokens were revoked before issuing new one
        verify(refreshTokenService).revokeAllUserTokens(1L);
        verify(refreshTokenService).createRefreshToken(testUser);
    }

    @Test
    @DisplayName("login() - throws BadCredentialsException for wrong password")
    void testLoginBadCredentials() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("johndoe")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refreshToken() - success: new access token returned with same refresh token")
    void testRefreshTokenSuccess() {
        // Arrange
        String tokenValue = "uuid-refresh-token-value";

        when(refreshTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenService.verifyExpiration(testRefreshToken))
                .thenReturn(testRefreshToken);
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpiryMs()).thenReturn(900000L);

        // Act
        AuthResponse response = authService.refreshToken(tokenValue);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo(tokenValue);
        assertThat(response.getUsername()).isEqualTo("johndoe");

        verify(refreshTokenRepository).findByToken(tokenValue);
        verify(refreshTokenService).verifyExpiration(testRefreshToken);
        verify(jwtService).generateAccessToken(testUser);
    }

    @Test
    @DisplayName("refreshToken() - throws TokenExpiredException when token is expired")
    void testRefreshTokenExpiredThrowsException() {
        // Arrange
        String tokenValue = "expired-token-value";
        RefreshToken expiredToken = RefreshToken.builder()
                .id(2L)
                .token(tokenValue)
                .user(testUser)
                .expiryDate(Instant.now().minusSeconds(3600)) // already expired
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken(tokenValue))
                .thenReturn(Optional.of(expiredToken));
        when(refreshTokenService.verifyExpiration(expiredToken))
                .thenThrow(new TokenExpiredException("Refresh token has expired. Please log in again."));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(tokenValue))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");

        verify(jwtService, never()).generateAccessToken(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout() - success: refresh token is revoked")
    void testLogoutSuccess() {
        // Arrange
        String tokenValue = "uuid-refresh-token-value";
        doNothing().when(refreshTokenService).revokeToken(tokenValue);

        // Act
        authService.logout(tokenValue);

        // Assert
        verify(refreshTokenService).revokeToken(tokenValue);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET CURRENT USER
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getCurrentUser() - success: returns UserInfoResponse with roles")
    void testGetCurrentUserSuccess() {
        // Arrange
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(testUser));

        // Act
        var response = authService.getCurrentUser("johndoe");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("johndoe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRoles()).contains("ROLE_EMPLOYEE");
    }
}
