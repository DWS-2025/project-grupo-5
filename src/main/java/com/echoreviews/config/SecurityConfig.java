package com.echoreviews.config;

import com.echoreviews.service.UserService;
import com.echoreviews.config.CustomAuthenticationSuccessHandler;
import com.echoreviews.config.BannedUserFilter;
import com.echoreviews.config.UserAgentValidationFilter;
import com.echoreviews.config.SqlInjectionFilter;
import com.echoreviews.security.JwtAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    // Comentar o eliminar temporalmente para verificar si este filtro es el problema
    // @Autowired
    // private SqlInjectionFilter sqlInjectionFilter;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        // Se usa BCrypt con factor 12 para mayor seguridad
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
        return new ServletListenerRegistrationBean<HttpSessionEventPublisher>(new HttpSessionEventPublisher());
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    /**
     * Define un matcher para identificar las rutas de recursos estáticos que deben excluirse
     * del filtro de inyección SQL.
     */
    private RequestMatcher staticResourcesMatcher() {
        return new OrRequestMatcher(
            new AntPathRequestMatcher("/css/**"),
            new AntPathRequestMatcher("/js/**"),
            new AntPathRequestMatcher("/images/**"),
            new AntPathRequestMatcher("/webjars/**"),
            new AntPathRequestMatcher("/fonts/**"),
            new AntPathRequestMatcher("/**/*.css"),
            new AntPathRequestMatcher("/**/*.js"),
            new AntPathRequestMatcher("/**/*.jpg"),
            new AntPathRequestMatcher("/**/*.jpeg"),
            new AntPathRequestMatcher("/**/*.png"),
            new AntPathRequestMatcher("/**/*.gif"),
            new AntPathRequestMatcher("/**/*.ico"),
            new AntPathRequestMatcher("/**/*.woff"),
            new AntPathRequestMatcher("/**/*.woff2"),
            new AntPathRequestMatcher("/**/*.ttf"),
            new AntPathRequestMatcher("/**/*.svg")
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Crear un matcher para todas las rutas excepto los recursos estáticos
        RequestMatcher nonStaticResourcesMatcher = new NegatedRequestMatcher(staticResourcesMatcher());
        
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .ignoringRequestMatchers("/api/**") // Ignorar CSRF para endpoints de API
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .requiresChannel(channel -> channel
                        .anyRequest().requiresSecure()
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/favorites/**", "/reviews/**").authenticated()
                        .requestMatchers("/login", "/auth/register", "/register", "/follow/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                        .requestMatchers("/",
                                "/css/**", 
                                "/js/**", 
                                "/images/**", 
                                "/fonts/**",
                                "/webjars/**", 
                                "/error", 
                                "/api/**",
                                "/users/**",
                                "/album/**", 
                                "/artists/**", 
                                "/top-albums/**", 
                                "/profile/**").permitAll()
                        .requestMatchers("/**/*.css",
                                "/**/*.js",
                                "/**/*.png",
                                "/**/*.jpg",
                                "/**/*.jpeg",
                                "/**/*.gif",
                                "/**/*.svg",
                                "/**/*.ico",
                                "/**/*.woff",
                                "/**/*.woff2",
                                "/**/*.ttf").permitAll()
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
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000))

                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https:; " +
                            "style-src 'self' 'unsafe-inline' https:; " +
                            "img-src 'self' data: https:; " +
                            "font-src 'self' data: https:; " +
                            "connect-src 'self' https:;"
                        ))
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .invalidSessionUrl("/login?invalid-session=true")
                        .maximumSessions(1)
                        .expiredUrl("/login?session-expired=true")
                )
                .addFilterAfter(bannedUserFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(userAgentValidationFilter, BannedUserFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, UserAgentValidationFilter.class);
                // Eliminar temporalmente este filtro para verificar si es la causa del problema
                // .addFilterAfter(sqlInjectionFilter, UserAgentValidationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://echoreviews.com", "https://www.echoreviews.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
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
