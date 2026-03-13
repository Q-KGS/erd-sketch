package com.erdsketch.support;

import com.erdsketch.auth.UserPrincipal;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

public class MockUserSupport {

    public static RequestPostProcessor mockUser(UUID userId) {
        UserPrincipal principal = new UserPrincipal(userId, userId + "@test.com", "hash");
        return SecurityMockMvcRequestPostProcessors.user(principal);
    }

    public static RequestPostProcessor mockUser() {
        return mockUser(UUID.randomUUID());
    }
}
