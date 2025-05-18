package com.echoreviews.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filter to prevent SQL injections
 * This filter inspects request parameters and headers
 * looking for suspicious SQL injection patterns
 * 
 * Note: Temporarily disabled to resolve CSS issues.
 */
// @Component // Temporarily commented to disable the filter
public class SqlInjectionFilter extends OncePerRequestFilter {

    // Suspicious SQL injection patterns
    private static final List<Pattern> SUSPICIOUS_PATTERNS = Arrays.asList(
            // Look for single quotes only if followed by SQL commands or in a suspicious context
            Pattern.compile(".*'\\s*(or|and|insert|update|delete|drop|alter|select|union)\\s+.*", Pattern.CASE_INSENSITIVE),
            // Detect SQL comments followed by commands
            Pattern.compile(".*--\\s*.*", Pattern.CASE_INSENSITIVE),
            // More specific destructive commands
            Pattern.compile(".*\\bdrop\\s+table\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bdelete\\s+from\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\binsert\\s+into\\b.*\\bvalues\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bupdate\\s+\\w+\\s+set\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bunion\\s+select\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bexec\\s+\\w+\\b.*", Pattern.CASE_INSENSITIVE),
            // Detect classic authentication bypass cases
            Pattern.compile(".*\\bor\\s+1\\s*=\\s*1\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\bor\\s+'\\s*'\\s*=\\s*'\\s*'\\b.*", Pattern.CASE_INSENSITIVE)
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Paths to exclude (static resources)
        String path = request.getRequestURI();
        
        // Improve static resource exclusion
        if (path.contains("/css/") || 
            path.contains("/js/") || 
            path.contains("/images/") || 
            path.contains("/webjars/") ||
            path.contains("/fonts/") ||
            path.endsWith(".css") || 
            path.endsWith(".js") || 
            path.endsWith(".jpg") || 
            path.endsWith(".jpeg") || 
            path.endsWith(".png") || 
            path.endsWith(".gif") || 
            path.endsWith(".ico") || 
            path.endsWith(".woff") || 
            path.endsWith(".woff2") || 
            path.endsWith(".ttf") || 
            path.endsWith(".svg")) {
            
            filterChain.doFilter(request, response);
            return;
        }

        // Check request parameters
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            String[] paramValues = request.getParameterValues(paramName);
            if (paramValues != null) {
                for (String paramValue : paramValues) {
                    if (isSqlInjectionSuspicious(paramValue)) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Possible SQL injection attempt detected.");
                        return;
                    }
                }
            }
        }

        // Check values in custom headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            // Exclude standard headers that might contain complex content
            if (!isStandardHeader(headerName)) {
                String headerValue = request.getHeader(headerName);
                if (isSqlInjectionSuspicious(headerValue)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Possible SQL injection attempt detected.");
                    return;
                }
            }
        }

        // If no injection is detected, continue with the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Checks if a string contains suspicious SQL injection patterns
     * @param value Value to check
     * @return true if suspicious, false if not
     */
    private boolean isSqlInjectionSuspicious(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        // Check against suspicious patterns
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                // Log the matching pattern and value for debugging
                System.out.println("Possible SQL injection detected: " + value);
                System.out.println("Matching pattern: " + pattern.pattern());
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if a header name is standard or not
     * Standard headers are not checked for SQL injection
     * @param headerName Header name
     * @return true if it's a standard header, false otherwise
     */
    private boolean isStandardHeader(String headerName) {
        List<String> standardHeaders = Arrays.asList(
                "host", "user-agent", "accept", "accept-language", "accept-encoding",
                "connection", "referer", "cookie", "content-length", "content-type",
                "origin", "cache-control", "pragma", "if-modified-since", "if-none-match",
                "x-requested-with", "x-forwarded-for", "x-forwarded-proto", "x-csrf-token",
                "authorization", "sec-fetch-dest", "sec-fetch-mode", "sec-fetch-site", 
                "sec-fetch-user", "upgrade-insecure-requests", "x-real-ip", "sec-ch-ua",
                "sec-ch-ua-mobile", "sec-ch-ua-platform", "access-control-request-method",
                "access-control-request-headers", "dnt", "date", "via", "x-xss-protection",
                "x-content-type-options"
        );
        return standardHeaders.contains(headerName.toLowerCase());
    }
} 