package com.echoreviews.config;

import com.echoreviews.model.User;
import com.echoreviews.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter that checks if a currently authenticated user is banned.
 * If so, it logs them out and redirects them to the login page with an error message.
 */
@Component
public class BannedUserFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Autowired
    public BannedUserFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && 
                !authentication.getPrincipal().equals("anonymousUser")) {
            
            String username = authentication.getName();
            Optional<User> userOptional = userRepository.findByUsername(username);
            
            if (userOptional.isPresent() && userOptional.get().isBanned()) {
                // User is banned, invalidate session and redirect to login page
                request.getSession().invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?error=banned");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 