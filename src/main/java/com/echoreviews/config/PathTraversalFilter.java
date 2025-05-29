package com.echoreviews.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Filter to detect and prevent path traversal attacks
 * Verifies suspicious patterns in requested URLs
 */

@Component
public class PathTraversalFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(PathTraversalFilter.class);
    
    // Path traversal patterns to detect
    private static final Pattern SUSPICIOUS_PATH_PATTERN = Pattern.compile(
            "\\.\\./|\\.\\.\\\\|/\\.\\./|\\\\\\.\\.\\\\|%2e%2e%2f|%252e%252e%252f|%c0%ae%c0%ae%c0%af",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = requestURI + (queryString != null ? "?" + queryString : "");
        
        // Log all requests for debugging
        // logger.info("Request received: {} {}", request.getMethod(), fullPath);
        
        // Print decoded URL to help with debugging
        String decodedUrl = java.net.URLDecoder.decode(fullPath, "UTF-8");
        // logger.info("Decoded URL: {}", decodedUrl);
        
        // Simple verification to detect basic path traversal patterns
        boolean containsPathTraversal = fullPath.contains("../") || 
                                        fullPath.contains("..\\") || 
                                        fullPath.contains("%2e%2e%2f") ||
                                        fullPath.contains("%2e%2e/") ||
                                        fullPath.contains("..%2f") ||
                                        decodedUrl.contains("../") ||
                                        decodedUrl.contains("..\\");
        
        // Check for suspicious path traversal pattern
        if (containsPathTraversal || SUSPICIOUS_PATH_PATTERN.matcher(fullPath).find() || SUSPICIOUS_PATH_PATTERN.matcher(decodedUrl).find()) {
            System.out.println("ALERT! POSSIBLE PATH TRAVERSAL ATTEMPT DETECTED: " + fullPath);
            logger.error("POSSIBLE PATH TRAVERSAL ATTEMPT DETECTED:");
            logger.error("IP: {}", request.getRemoteAddr());
            logger.error("Method: {}", request.getMethod());
            logger.error("Full path: {}", fullPath);
            logger.error("User-Agent: {}", request.getHeader("User-Agent"));
            
            // Return 400 Bad Request error
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request: possible path traversal");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
} 