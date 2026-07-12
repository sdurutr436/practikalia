package practikalia.usuario;

import practikalia.etiqueta.EtiquetaDto;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * Gestión de cuentas de usuario: alta (solo profesor/admin), perfil de
 * grado/año del alumno y sus etiquetas de interés. La mayoría de rutas están
 * restringidas a profesor/admin en {@code SecurityConfig}; las de etiquetas
 * son la excepción y añaden control "propio o profesor" en el servicio.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Crear un usuario", description = "Solo profesor/admin. Crear con rol `PROFESOR` requiere además "
            + "que quien lo crea sea admin. El correo debe estar en la whitelist de la instancia. "
            + "Devuelve una contraseña temporal en claro (solo en esta respuesta) que el usuario deberá cambiar en su primer login.")
    @ApiResponse(responseCode = "403", description = "Quien crea no es admin e intenta crear un `PROFESOR`, o el correo no está permitido")
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado")
    @PostMapping
    public ResponseEntity<CrearUsuarioResponse> crear(Authentication authentication, @Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioDto creador = usuarioService.buscarPorCorreo(authentication.getName());
        CrearUsuarioResponse response = usuarioService.crearUsuario(request, creador);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Actualizar el grado/año de un usuario", description = "Solo profesor/admin. Reemplaza el perfil completo (grado y año a la vez).")
    @ApiResponse(responseCode = "404", description = "El usuario o el grado indicado no existen")
    @PutMapping("/{id}/grado")
    public ResponseEntity<UsuarioGradoDto> actualizarGrado(@PathVariable Long id, @Valid @RequestBody ActualizarGradoRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarGrado(id, request));
    }

    @Operation(summary = "Reemplazar las etiquetas de interés de un usuario", description = "Accesible por el propio "
            + "usuario o por cualquier profesor/admin. Reemplazo completo de la lista: enviar `[]` la vacía.")
    @ApiResponse(responseCode = "403", description = "Un alumno intenta editar las etiquetas de otro usuario")
    @ApiResponse(responseCode = "404", description = "El usuario no existe, o alguna de las etiquetas indicadas no existe")
    @PutMapping("/{id}/etiquetas")
    public ResponseEntity<List<EtiquetaDto>> actualizarEtiquetas(Authentication authentication,
            @PathVariable Long id, @Valid @RequestBody ActualizarEtiquetasRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarEtiquetas(
                id, request, esProfesor(authentication), authentication.getName()));
    }

    @Operation(summary = "Consultar las etiquetas de interés de un usuario", description = "Accesible por el propio usuario o por cualquier profesor/admin.")
    @ApiResponse(responseCode = "403", description = "Un alumno intenta consultar las etiquetas de otro usuario")
    @ApiResponse(responseCode = "404", description = "El usuario no existe")
    @GetMapping("/{id}/etiquetas")
    public ResponseEntity<List<EtiquetaDto>> obtenerEtiquetas(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerEtiquetas(
                id, esProfesor(authentication), authentication.getName()));
    }

    private boolean esProfesor(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROFESOR"));
    }
}
