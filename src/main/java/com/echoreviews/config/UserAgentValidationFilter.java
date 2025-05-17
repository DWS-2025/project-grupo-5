package com.echoreviews.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to validate that the user agent matches the one used during authentication.
 * This prevents session hijacking by ensuring that a stolen session ID cannot be used
 * from a different browser or device.
 */
@Component
public class UserAgentValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        
        if (session != null && session.getAttribute("USER_AGENT") != null) {
            String originalUA = (String) session.getAttribute("USER_AGENT");
            String currentUA = request.getHeader("User-Agent");

            String originalIP = (String) session.getAttribute("IP_ADDRESS");
            String currentIP = request.getRemoteAddr();

            if (!originalUA.equals(currentUA) || !originalIP.equals(currentIP)) {
                // Possible session hijacking
                session.invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?hijacked=true");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 