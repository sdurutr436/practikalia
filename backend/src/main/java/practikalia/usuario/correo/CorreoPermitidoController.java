package practikalia.usuario.correo;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * Whitelist de correos permitidos, gestionada desde la pantalla de
 * configuración. Solo admin ({@code SecurityConfig}): son correos de
 * personas, dato personal.
 */
@RestController
@RequestMapping("/api/correos-permitidos")
public class CorreoPermitidoController {

    private final CorreoPermitidoService correoPermitidoService;

    public CorreoPermitidoController(CorreoPermitidoService correoPermitidoService) {
        this.correoPermitidoService = correoPermitidoService;
    }

    @Operation(summary = "Listar la whitelist", description = "Solo admin. Ordenada alfabéticamente.")
    @GetMapping
    public ResponseEntity<List<CorreoPermitidoDto>> listar() {
        return ResponseEntity.ok(correoPermitidoService.listar());
    }

    @Operation(summary = "Añadir un correo a la whitelist", description = "Solo admin. Se guarda en minúsculas.")
    @ApiResponse(responseCode = "409", description = "Ese correo ya está en la whitelist")
    @PostMapping
    public ResponseEntity<CorreoPermitidoDto> crear(@Valid @RequestBody CrearCorreoPermitidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(correoPermitidoService.crear(request));
    }

    @Operation(summary = "Quitar un correo de la whitelist", description = "Solo admin. No borra la cuenta que ya "
            + "se diera de alta con él, pero le impide volver a entrar.")
    @ApiResponse(responseCode = "404", description = "Ese id no está en la whitelist")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrar(@PathVariable Long id) {
        correoPermitidoService.borrar(id);
        return ResponseEntity.noContent().build();
    }
}
