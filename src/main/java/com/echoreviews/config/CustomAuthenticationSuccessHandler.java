package com.echoreviews.config;

import com.echoreviews.dto.UserDTO;
import com.echoreviews.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom handler for actions after successful authentication.
 * Its main function is to obtain the complete UserDTO of the authenticated user
 * and store it in the HttpSession to be available throughout the application,
 * maintaining compatibility with parts of the code that depend on this session attribute.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    @Autowired
    public CustomAuthenticationSuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username;
        // Get the username from Spring Security's Principal object
        if (authentication.getPrincipal() instanceof UserDetails) {
            username = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            username = authentication.getPrincipal().toString();
        }

        // Load the complete UserDTO using the user service
        UserDTO userDTO = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found during session setup after login: " + username));

        // Store the UserDTO in the HTTP session
        HttpSession session = request.getSession();
        session.setAttribute("user", userDTO);
        
        // Store the user agent and IP address in the session for validation against session hijacking
        session.setAttribute("USER_AGENT", request.getHeader("User-Agent"));
        session.setAttribute("IP_ADDRESS", request.getRemoteAddr());

        // Redirect the user to the home page after successful login.
        // Spring Security could handle more complex redirections (to the originally requested URL)
        // if SavedRequestAwareAuthenticationSuccessHandler were used, but for this case,
        // a redirect to root is sufficient and consistent with the previous defaultSuccessUrl configuration.
        response.sendRedirect(request.getContextPath() + "/");
    }
} 