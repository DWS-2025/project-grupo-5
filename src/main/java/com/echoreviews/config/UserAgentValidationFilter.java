package com.echoreviews.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(UserAgentValidationFilter.class);

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
                String sessionId = session.getId();
                logger.warn("POSSIBLE SESSION HIJACKING ATTEMPT DETECTED:");
                logger.warn("Session ID: {}", sessionId);
                logger.warn("Original User Agent: {}", originalUA);
                logger.warn("Current User Agent: {}", currentUA);
                logger.warn("Original IP: {}", originalIP);
                logger.warn("Current IP: {}", currentIP);
                logger.warn("Access path: {}", request.getRequestURI());
                
                session.invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?hijacked=true");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 