package com.freelance.projectmanager.config;

import com.freelance.projectmanager.model.User;
import com.freelance.projectmanager.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/admin/**", "/api/auth/**","/api/newsletter/**", "/login/**", "/oauth2/**", "/error").permitAll()
                .requestMatchers("/api/messages/**", "/api/jobs/**", "/api/proposals/**", "/api/bank/**", "/api/wallet/**").permitAll()
                .requestMatchers("/", "/html/**", "/css/**", "/js/**", "/script/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    // Yahan hum 401 bhejenge taaki pata chale unauthorized hai, 404 nahi
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                })
            )
            .oauth2Login(oauth -> oauth
                .successHandler(oauthSuccessHandler())
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // SAARE REQUIRED DOMAINS ADD KIYE HAIN
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:5500", 
            "http://127.0.0.1:5500", 
            "https://hir-bee-3nwb.vercel.app", 
            "https://hir-bee.vercel.app",
            "https://*.vercel.app"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
public AuthenticationSuccessHandler oauthSuccessHandler() {
    return (request, response, authentication) -> {
        org.springframework.security.oauth2.core.user.OAuth2User principal = 
            (org.springframework.security.oauth2.core.user.OAuth2User) authentication.getPrincipal();
        
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;

        // PRODUCTION CHECK: Agar server Render par chal raha hai toh Vercel par bhejien
        String host = request.getHeader("Host");
        String baseUrl = (host != null && host.contains("onrender.com")) 
                         ? "https://hir-bee.vercel.app" 
                         : "http://127.0.0.1:5500/frontend/html"; 

        if (userOpt.isEmpty()) {
            user = new User();
            user.setEmail(email);
            user.setFullName(name != null ? name : "Google User");
            user.setPassword("OAUTH2_USER"); 
            user.setEnabled(true);
            userRepository.save(user);
            
            response.sendRedirect(baseUrl + "/role.html?google_email=" + email);
        } else {
            user = userOpt.get();
            String targetUrl;
            if (user.getRole() == null) {
                targetUrl = baseUrl + "/role.html?google_email=" + email;
            } else if (user.getRole() == User.Role.ADMIN) {
                targetUrl = baseUrl + "/admin-dashboard.html";
            } else if (user.getRole() == User.Role.CLIENT) {
                targetUrl = baseUrl + "/client-dashboard.html";
            } else {
                targetUrl = baseUrl + "/freelancer-dashboard.html";
            }
            response.sendRedirect(targetUrl);
        }
    };
}
}
