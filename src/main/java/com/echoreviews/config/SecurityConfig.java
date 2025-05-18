package com.echoreviews.config;

import com.echoreviews.service.UserService;
import com.echoreviews.config.CustomAuthenticationSuccessHandler;
import com.echoreviews.config.BannedUserFilter;
import com.echoreviews.config.UserAgentValidationFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserService userService;

    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    
    @Autowired
    private BannedUserFilter bannedUserFilter;
    
    @Autowired
    private UserAgentValidationFilter userAgentValidationFilter;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<HttpSessionEventPublisher>(new HttpSessionEventPublisher());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                )
                .requiresChannel(channel -> channel
                        .anyRequest().requiresSecure()
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/favorites/**", "/reviews/**").authenticated()
                        .requestMatchers("/login", "/auth/register", "/register").permitAll()
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/webjars/**", "/error", "/api/**", "/album/**", "/artists/**", "/top-albums/**", "/profile/**").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .headers(headers -> headers
                        .frameOptions().sameOrigin()
                        .httpStrictTransportSecurity().maxAgeInSeconds(31536000)
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .invalidSessionUrl("/login?invalid-session=true")
                        .maximumSessions(1)
                        .expiredUrl("/login?session-expired=true")
                )
                .addFilterAfter(bannedUserFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(userAgentValidationFilter, BannedUserFilter.class);

        return http.build();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService)
            .passwordEncoder(passwordEncoder());
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setSessionAttributeName("_csrf");
        return repository;
    }
}
