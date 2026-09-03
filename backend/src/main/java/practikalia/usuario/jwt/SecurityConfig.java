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
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro").permitAll()
                        .requestMatchers("/api/auth/cambiar-contrasena").hasAnyAuthority(
                                JwtService.AUTORIDAD_CAMBIO_PENDIENTE, "ROLE_ALUMNO", "ROLE_PROFESOR")
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                                .hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/grados/publico").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/grados").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        // El nombre y el logo los pinta el acceso antes de que nadie haya
                        // entrado; renombrar el centro, subir el logo y la whitelist son de
                        // admin — son correos de personas, dato personal.
                        .requestMatchers(HttpMethod.GET, "/api/centro").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/centro").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/centro/logo").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/correos-permitidos").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/correos-permitidos").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/correos-permitidos/*").hasAuthority("ADMIN")
                        // El catálogo plano lo lee cualquiera (formulario de empresa, intereses
                        // del alumnado); montarlo y desmontarlo es solo de admin.
                        .requestMatchers(HttpMethod.GET, "/api/etiquetas/arbol").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/etiquetas").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/etiquetas/*").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/etiquetas/*").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/etiquetas").hasAnyAuthority("ROLE_ALUMNO", "ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/*/etiquetas").hasAnyAuthority("ROLE_ALUMNO", "ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/*/activar").hasAuthority("ADMIN")
                        .requestMatchers("/api/usuarios/**").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/empresas/*/interes").hasAuthority("ROLE_ALUMNO")
                        .requestMatchers(HttpMethod.DELETE, "/api/empresas/*/interes").hasAuthority("ROLE_ALUMNO")
                        .requestMatchers(HttpMethod.GET, "/api/empresas/*/interesados").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/empresas/afinidad").hasAuthority("ROLE_ALUMNO")
                        .requestMatchers(HttpMethod.GET, "/api/alumnos/*/afinidad").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        // Sin estos, el listado de alumnado caería en anyRequest(), que admite ROLE_ALUMNO.
                        .requestMatchers(HttpMethod.GET, "/api/alumnos").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/alumnos/curso").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/alumnos/cursos").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/alumnos/*/asignacion").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/alumnos/plantilla.csv").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/alumnos/importar").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/alumnos").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/alumnos/*").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        // El profesorado se lee entre sí, pero no se edita entre sí: dar de
                        // alta y editar una ficha es solo del admin.
                        .requestMatchers(HttpMethod.GET, "/api/profesores").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/profesores").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/profesores/*").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/empresas/**").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/empresas/**").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/empresas/*/asignaciones").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/asignaciones").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/asignaciones/**").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/panel/resumen").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reviews/pendientes").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reviews").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/*/revertir").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/*/moderar").hasAnyAuthority("ROLE_PROFESOR", "ADMIN")
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
