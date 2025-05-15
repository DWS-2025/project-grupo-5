package com.musicstore.security;

import com.musicstore.dto.UserDTO;
import com.musicstore.service.UserService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserService userService;

    public CustomAuthenticationProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        System.out.println("[CustomAuthenticationProvider] Attempting to authenticate user: " + username);

        UserDTO user = userService.authenticateUser(username, password)
                .orElseThrow(() -> {
                    System.out.println("[CustomAuthenticationProvider] Authentication failed for user: " + username + " (Invalid credentials)");
                    return new BadCredentialsException("Invalid username or password");
                });

        System.out.println("[CustomAuthenticationProvider] User DTO retrieved: " + user.username() + ", isAdmin: " + user.isAdmin());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        if (user.isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        System.out.println("[CustomAuthenticationProvider] Authorities being set for " + user.username() + ": " + authorities);

        System.out.println("[CustomAuthenticationProvider] About to create UsernamePasswordAuthenticationToken.");
        UsernamePasswordAuthenticationToken successToken = null;
        try {
            successToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
            
            System.out.println("[CustomAuthenticationProvider] UsernamePasswordAuthenticationToken object created successfully.");
            System.out.println("[CustomAuthenticationProvider] Successfully created authentication token for: " + user.username() + ". Token details: " + successToken);
            
            return successToken;
        } catch (Exception e) {
            System.err.println("[CustomAuthenticationProvider] CRITICAL ERROR creating or logging token for user '" + username + "': " + e.getMessage());
            e.printStackTrace(System.err);
            throw new BadCredentialsException("Internal error during token creation for user '" + username + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
} 