package com.erdsketch.auth;

import com.erdsketch.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    @Getter
    private final UUID id;
    private final String email;
    private final String passwordHash;

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return Collections.emptyList(); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return id.toString(); }
}
