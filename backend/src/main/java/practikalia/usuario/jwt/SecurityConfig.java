package practikalia.usuario.jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtSecurityHandlers jwtSecurityHandlers;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JwtSecurityHandlers jwtSecurityHandlers) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtSecurityHandlers = jwtSecurityHandlers;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/cambiar-contrasena").hasAnyAuthority(
                                JwtService.AUTORIDAD_CAMBIO_PENDIENTE, "ROLE_ALUMNO", "ROLE_PROFESOR")
                        .requestMatchers("/api/usuarios/**").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/empresas/**").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/empresas/**").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/empresas/*/asignaciones").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/asignaciones").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/asignaciones/**").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .anyRequest().hasAnyAuthority("ROLE_ALUMNO", "ROLE_PROFESOR", "ADMIN"))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'")))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(jwtSecurityHandlers)
                        .accessDeniedHandler(jwtSecurityHandlers))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
