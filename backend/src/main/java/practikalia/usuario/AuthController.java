package practikalia.usuario;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "practikalia_token";
    private static final Duration DURACION_COOKIE = Duration.ofDays(7);

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        Usuario usuario = usuarioService.login(request, httpRequest.getRemoteAddr());
        String token = usuarioService.emitirToken(usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, construirCookie(token, DURACION_COOKIE).toString())
                .body(new LoginResponse(usuario.getRol(), usuario.isEsAdmin(), usuario.isDebeCambiarContrasena()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, construirCookie("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        Usuario usuario = usuarioService.buscarPorCorreo(authentication.getName());
        return ResponseEntity.ok(
                new MeResponse(usuario.getCorreo(), usuario.getRol(), usuario.isEsAdmin(), usuario.isDebeCambiarContrasena()));
    }

    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<Void> cambiarContrasena(Authentication authentication, @Valid @RequestBody CambiarContrasenaRequest request) {
        usuarioService.cambiarContrasena(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie construirCookie(String token, Duration duracion) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(duracion)
                .build();
    }
}
