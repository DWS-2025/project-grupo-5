package com.musicstore.config;

import com.musicstore.security.CustomAuthenticationProvider;
import com.musicstore.security.CustomAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import jakarta.servlet.http.HttpSession;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomAuthenticationProvider authenticationProvider;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    public SecurityConfig(CustomAuthenticationProvider authenticationProvider,
                          CustomAuthenticationSuccessHandler authenticationSuccessHandler) {
        this.authenticationProvider = authenticationProvider;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                )
                /* // Comentado para desarrollo local si se usa HTTP
                .requiresChannel(channel -> channel
                        .anyRequest().requiresSecure()
                )
                */
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/auth/register", "/", "/artists", "/albums", "/css/**", "/js/**", "/images/**", "/top-albums", "/api/**", "/reviews/user/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID") // Nombre correcto de la cookie de sesión
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/login")
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false) // Permitir iniciar sesión desde otro lugar
                        .expiredUrl("/login")
                )
                .headers(headers -> headers
                        .frameOptions().sameOrigin()
                        // .httpStrictTransportSecurity().maxAgeInSeconds(31536000) // Comentado para desarrollo local
                )
                .authenticationProvider(authenticationProvider);

        return http.build();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setSessionAttributeName("_csrf");
        return repository;
    }
}
