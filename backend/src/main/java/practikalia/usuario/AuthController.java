package practikalia.usuario;

import practikalia.usuario.jwt.JwtService;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Autenticación: login/logout por cookie JWT httpOnly (sin cabecera
 * {@code Authorization}) y cambio de contraseña. Público solo el login;
 * el resto requiere sesión ya iniciada.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Duration DURACION_COOKIE = Duration.ofDays(7);

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Iniciar sesión", description = "Público. Valida correo/contraseña y, si son correctos, "
            + "fija la cookie JWT httpOnly con el token de sesión. Si la cuenta tiene "
            + "`debeCambiarContrasena=true`, el token emitido solo autoriza "
            + "`POST /api/auth/cambiar-contrasena` (resto de endpoints devuelven 403 hasta el cambio).")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas, o el campo honeypot `web` viene relleno (indicio de bot)")
    @ApiResponse(responseCode = "403", description = "La cuenta no está activa o su correo ya no está en la whitelist permitida")
    @ApiResponse(responseCode = "429", description = "Cuenta bloqueada temporalmente tras varios intentos fallidos consecutivos")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResultado resultado = usuarioService.login(request, httpRequest.getRemoteAddr());
        UsuarioDto usuario = resultado.usuario();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, construirCookie(resultado.token(), DURACION_COOKIE).toString())
                .body(new LoginResponse(usuario.rol(), usuario.esAdmin(), usuario.debeCambiarContrasena()));
    }

    @Operation(summary = "Cerrar sesión", description = "Invalida la cookie de sesión en el cliente (no hay blacklist de tokens en servidor).")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, construirCookie("", Duration.ZERO).toString())
                .build();
    }

    @Operation(summary = "Perfil del usuario autenticado", description = "Refleja siempre las etiquetas de interés actuales del usuario.")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        UsuarioDto usuario = usuarioService.buscarPorCorreo(authentication.getName());
        return ResponseEntity.ok(
                new MeResponse(usuario.correo(), usuario.rol(), usuario.esAdmin(), usuario.debeCambiarContrasena(),
                        usuario.etiquetas()));
    }

    @Operation(summary = "Cambiar la contraseña propia", description = "Único endpoint accesible con un token restringido "
            + "(`debeCambiarContrasena=true`). Tras el cambio, ese flag pasa a `false`.")
    @ApiResponse(responseCode = "401", description = "La contraseña actual indicada no es correcta")
    @ApiResponse(responseCode = "400", description = "La contraseña nueva no cumple la política (mínimo 8 caracteres, mayúscula, minúscula, número y carácter especial)")
    @PostMapping("/cambiar-contrasena")
    public ResponseEntity<Void> cambiarContrasena(Authentication authentication, @Valid @RequestBody CambiarContrasenaRequest request) {
        usuarioService.cambiarContrasena(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    private ResponseCookie construirCookie(String token, Duration duracion) {
        return ResponseCookie.from(JwtService.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(duracion)
                .build();
    }
}
