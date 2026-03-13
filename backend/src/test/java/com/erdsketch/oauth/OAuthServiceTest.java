package com.erdsketch.oauth;

import com.erdsketch.auth.JwtService;
import com.erdsketch.auth.TokenResponse;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private OAuthService oAuthService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .email("test@gmail.com")
                .displayName("Test User")
                .passwordHash(null)
                .oauthProvider("google")
                .oauthSubject("google-123")
                .build();
        // Use reflection to set id since @GeneratedValue won't work without JPA
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(savedUser, UUID.randomUUID());
        } catch (Exception e) {
            // ignore
        }
    }

    // B-OAUTH-01: processOAuthUser (google) → User 생성 + JWT 발급
    @Test
    void processOAuthUser_google_createsUserAndIssuesJwt() {
        OAuthUserInfo info = new OAuthUserInfo("google", "google-123", "test@gmail.com", "Test User", "http://photo");

        given(userRepository.findByOauthProviderAndOauthSubject("google", "google-123"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtService.generateAccessToken(anyString(), anyString())).willReturn("access-token");
        given(jwtService.generateRefreshToken(anyString(), anyString())).willReturn("refresh-token");

        TokenResponse result = oAuthService.processOAuthUser(info);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
    }

    // B-OAUTH-02: processOAuthUser (github) → User 생성 + JWT 발급
    @Test
    void processOAuthUser_github_createsUserAndIssuesJwt() {
        OAuthUserInfo info = new OAuthUserInfo("github", "gh-456", "dev@github.noreply.com", "devuser", "http://avatar");

        User ghUser = User.builder()
                .email("dev@github.noreply.com")
                .displayName("devuser")
                .passwordHash(null)
                .oauthProvider("github")
                .oauthSubject("gh-456")
                .build();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ghUser, UUID.randomUUID());
        } catch (Exception e) { /* ignore */ }

        given(userRepository.findByOauthProviderAndOauthSubject("github", "gh-456"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("dev@github.noreply.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(ghUser);
        given(jwtService.generateAccessToken(anyString(), anyString())).willReturn("access-gh");
        given(jwtService.generateRefreshToken(anyString(), anyString())).willReturn("refresh-gh");

        TokenResponse result = oAuthService.processOAuthUser(info);

        assertThat(result.accessToken()).isEqualTo("access-gh");
        verify(userRepository).save(any(User.class));
    }

    // B-OAUTH-03: 동일 이메일 기존 계정 → 기존 User 연동
    @Test
    void processOAuthUser_existingEmail_linksAccount() {
        OAuthUserInfo info = new OAuthUserInfo("google", "google-new", "existing@example.com", "Existing", null);

        User existingUser = User.builder()
                .email("existing@example.com")
                .displayName("Existing")
                .passwordHash("$2a$hash")
                .build();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(existingUser, UUID.randomUUID());
        } catch (Exception e) { /* ignore */ }

        given(userRepository.findByOauthProviderAndOauthSubject("google", "google-new"))
                .willReturn(Optional.empty());
        given(userRepository.findByEmail("existing@example.com")).willReturn(Optional.of(existingUser));
        given(userRepository.save(any(User.class))).willReturn(existingUser);
        given(jwtService.generateAccessToken(anyString(), anyString())).willReturn("access-linked");
        given(jwtService.generateRefreshToken(anyString(), anyString())).willReturn("refresh-linked");

        TokenResponse result = oAuthService.processOAuthUser(info);

        assertThat(result.accessToken()).isEqualTo("access-linked");
        // Verify oauthProvider and oauthSubject were updated
        assertThat(existingUser.getOauthProvider()).isEqualTo("google");
        assertThat(existingUser.getOauthSubject()).isEqualTo("google-new");
    }

    // B-OAUTH-04: extractUserInfo에서 유효하지 않은 provider → exception
    @Test
    void extractUserInfo_unknownProvider_throwsException() {
        OAuth2User mockUser = mock(OAuth2User.class);
        given(mockUser.getAttributes()).willReturn(Map.of("id", "123"));

        assertThatThrownBy(() -> oAuthService.extractUserInfo(mockUser, "twitter"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported OAuth provider");
    }

    // B-OAUTH-05: 존재하지 않는 path → 테스트 불필요 (controller 레벨)
    // (skip)

    // B-OAUTH-06: 이미 연동된 계정 재연동 → 기존 user_id 유지
    @Test
    void processOAuthUser_alreadyLinked_keepsSameUserId() {
        UUID existingId = UUID.randomUUID();
        User existingUser = User.builder()
                .email("linked@gmail.com")
                .displayName("Linked User")
                .passwordHash(null)
                .oauthProvider("google")
                .oauthSubject("google-123")
                .build();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(existingUser, existingId);
        } catch (Exception e) { /* ignore */ }

        OAuthUserInfo info = new OAuthUserInfo("google", "google-123", "linked@gmail.com", "Linked User", null);

        given(userRepository.findByOauthProviderAndOauthSubject("google", "google-123"))
                .willReturn(Optional.of(existingUser));
        given(userRepository.save(any(User.class))).willReturn(existingUser);
        given(jwtService.generateAccessToken(anyString(), anyString())).willReturn("access-relink");
        given(jwtService.generateRefreshToken(anyString(), anyString())).willReturn("refresh-relink");

        TokenResponse result = oAuthService.processOAuthUser(info);

        assertThat(result.accessToken()).isEqualTo("access-relink");
        // Verify same user id
        verify(userRepository, never()).findByEmail(anyString());
        assertThat(existingUser.getId()).isEqualTo(existingId);
    }

    // extractUserInfo Google test
    @Test
    void extractUserInfo_google_extractsCorrectly() {
        OAuth2User mockGoogleUser = mock(OAuth2User.class);
        Map<String, Object> attrs = Map.of(
                "sub", "google-123",
                "email", "test@gmail.com",
                "name", "Test User",
                "picture", "http://photo");
        given(mockGoogleUser.getAttributes()).willReturn(attrs);

        OAuthUserInfo info = oAuthService.extractUserInfo(mockGoogleUser, "google");

        assertThat(info.provider()).isEqualTo("google");
        assertThat(info.subject()).isEqualTo("google-123");
        assertThat(info.email()).isEqualTo("test@gmail.com");
        assertThat(info.displayName()).isEqualTo("Test User");
        assertThat(info.avatarUrl()).isEqualTo("http://photo");
    }

    // extractUserInfo GitHub test
    @Test
    void extractUserInfo_github_extractsCorrectly() {
        OAuth2User mockGithubUser = mock(OAuth2User.class);
        Map<String, Object> attrs = new java.util.HashMap<>();
        attrs.put("id", 456);
        attrs.put("login", "devuser");
        attrs.put("avatar_url", "http://avatar");
        attrs.put("email", null);
        given(mockGithubUser.getAttributes()).willReturn(attrs);

        OAuthUserInfo info = oAuthService.extractUserInfo(mockGithubUser, "github");

        assertThat(info.provider()).isEqualTo("github");
        assertThat(info.subject()).isEqualTo("456");
        assertThat(info.email()).isEqualTo("456@github.noreply.com"); // fallback
        assertThat(info.displayName()).isEqualTo("devuser");
    }
}
