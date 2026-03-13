package com.erdsketch.oauth;

import com.erdsketch.auth.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuthService oAuthService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuthUserInfo info = oAuthService.extractUserInfo(
                (OAuth2User) token.getPrincipal(), token.getAuthorizedClientRegistrationId());
        TokenResponse tokens = oAuthService.processOAuthUser(info);
        String redirectUrl = "/oauth/callback?access_token=" + tokens.accessToken()
                + "&refresh_token=" + tokens.refreshToken();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
