package com.erdsketch.auth;

import com.erdsketch.support.BaseIntegrationTest;
import com.erdsketch.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthServiceTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    // ───── B-AUTH-01: 정상 회원가입 ─────
    @Test
    void B_AUTH_01_정상_회원가입() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"pass1234","displayName":"Alice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("a@b.com"))
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty());
    }

    // ───── B-AUTH-02: 중복 이메일 가입 ─────
    @Test
    void B_AUTH_02_중복_이메일_가입() throws Exception {
        authService.register(new RegisterRequest("dup@b.com", "pass1234", "Alice"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dup@b.com","password":"pass1234","displayName":"Bob"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Email already exists"));
    }

    // ───── B-AUTH-03: 비밀번호 8자 미만 ─────
    @Test
    void B_AUTH_03_비밀번호_8자_미만() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"short@b.com","password":"1234567","displayName":"Alice"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ───── B-AUTH-04: 정상 로그인 ─────
    @Test
    void B_AUTH_04_정상_로그인() throws Exception {
        authService.register(new RegisterRequest("login@b.com", "pass1234", "Alice"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@b.com","password":"pass1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokens.refreshToken").isNotEmpty());
    }

    // ───── B-AUTH-05: 잘못된 비밀번호 ─────
    @Test
    void B_AUTH_05_잘못된_비밀번호() throws Exception {
        authService.register(new RegisterRequest("wrong@b.com", "pass1234", "Alice"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrong@b.com","password":"wrongpass"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ───── B-AUTH-06: 존재하지 않는 이메일 로그인 ─────
    @Test
    void B_AUTH_06_존재하지_않는_이메일() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"notexist@b.com","password":"pass1234"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ───── B-AUTH-07: 토큰 갱신 ─────
    @Test
    void B_AUTH_07_토큰_갱신() throws Exception {
        AuthResponse auth = authService.register(new RegisterRequest("refresh@b.com", "pass1234", "Alice"));
        String refreshToken = auth.tokens().refreshToken();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    // ───── B-AUTH-08: 만료/유효하지 않은 refreshToken ─────
    @Test
    void B_AUTH_08_유효하지_않은_refreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"invalid.token.here\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ───── B-AUTH-09: /auth/me 인증된 요청 ─────
    @Test
    void B_AUTH_09_me_인증된_요청() throws Exception {
        AuthResponse auth = authService.register(new RegisterRequest("me@b.com", "pass1234", "Alice"));
        String accessToken = auth.tokens().accessToken();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@b.com"));
    }

    // ───── B-AUTH-10: /auth/me 토큰 없음 ─────
    @Test
    void B_AUTH_10_me_토큰_없음() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ───── 서비스 단위 검증 ─────
    @Test
    void 회원가입_서비스_레벨_중복이메일_예외() {
        authService.register(new RegisterRequest("svc@b.com", "pass1234", "Alice"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest("svc@b.com", "pass1234", "Bob")));
    }

    @Test
    void 회원가입_후_DB에_사용자_저장됨() {
        authService.register(new RegisterRequest("db@b.com", "pass1234", "Alice"));
        assertThat(userRepository.existsByEmail("db@b.com")).isTrue();
    }
}
