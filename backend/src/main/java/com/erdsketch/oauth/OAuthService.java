package com.erdsketch.oauth;

import com.erdsketch.auth.JwtService;
import com.erdsketch.auth.TokenResponse;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Transactional
    public TokenResponse processOAuthUser(OAuthUserInfo info) {
        // 1. oauth_provider + oauth_subject로 기존 User 조회
        Optional<User> existing = userRepository.findByOauthProviderAndOauthSubject(
                info.provider(), info.subject());

        User user;
        if (existing.isPresent()) {
            user = existing.get();
        } else {
            // 2. email로 기존 계정 조회 (이메일 계정 연동)
            Optional<User> byEmail = info.email() != null
                    ? userRepository.findByEmail(info.email())
                    : Optional.empty();

            if (byEmail.isPresent()) {
                user = byEmail.get();
            } else {
                // 3. 새 User 생성 (passwordHash = null)
                String email = info.email() != null
                        ? info.email()
                        : info.subject() + "@" + info.provider() + ".noreply.com";
                String displayName = info.displayName() != null ? info.displayName() : email;
                user = User.builder()
                        .email(email)
                        .displayName(displayName)
                        .avatarUrl(info.avatarUrl())
                        .passwordHash(null)
                        .build();
            }
        }

        // 4. oauthProvider, oauthSubject 업데이트
        user.setOauthProvider(info.provider());
        user.setOauthSubject(info.subject());
        if (info.avatarUrl() != null) {
            user.setAvatarUrl(info.avatarUrl());
        }
        User savedUser = userRepository.save(user);

        // 5. JWT 발급
        String accessToken = jwtService.generateAccessToken(savedUser.getId().toString(), savedUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId().toString(), savedUser.getEmail());

        // 6. TokenResponse 반환
        return new TokenResponse(accessToken, refreshToken);
    }

    OAuthUserInfo extractUserInfo(OAuth2User oAuth2User, String registrationId) {
        Map<String, Object> attrs = oAuth2User.getAttributes();

        return switch (registrationId.toLowerCase()) {
            case "google" -> {
                String subject = String.valueOf(attrs.get("sub"));
                String email = (String) attrs.get("email");
                String name = (String) attrs.get("name");
                String picture = (String) attrs.get("picture");
                yield new OAuthUserInfo("google", subject, email, name, picture);
            }
            case "github" -> {
                Object idObj = attrs.get("id");
                String subject = idObj != null ? String.valueOf(idObj) : null;
                String email = (String) attrs.get("email");
                String login = (String) attrs.get("login");
                String avatarUrl = (String) attrs.get("avatar_url");
                // github email이 null일 수 있음 → fallback
                if (email == null && subject != null) {
                    email = subject + "@github.noreply.com";
                }
                yield new OAuthUserInfo("github", subject, email, login, avatarUrl);
            }
            default -> throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
        };
    }
}
