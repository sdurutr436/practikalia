package practikalia.usuario.alumnado;

import practikalia.common.PaginaDto;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * Listado de alumnado del centro: la pantalla de gestión, con su edición de
 * ficha y el alta masiva por CSV. Restringido a profesor/admin en
 * {@code SecurityConfig}; confirmar una cuenta sigue siendo cosa del admin, con
 * {@code PUT /api/usuarios/{id}/activar}.
 */
@RestController
@RequestMapping("/api/alumnos")
public class AlumnoController {

    private final AlumnoService alumnoService;

    public AlumnoController(AlumnoService alumnoService) {
        this.alumnoService = alumnoService;
    }

    @Operation(summary = "Listar alumnado", description = "Solo profesor/admin. Paginado y ordenado por apellido. "
            + "`activo` filtra las pastillas de la pantalla: `true` confirmados, `false` pendientes de confirmar, "
            + "sin el parámetro salen todos.")
    @GetMapping
    public ResponseEntity<PaginaDto<AlumnoDto>> listar(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int tamano) {
        return ResponseEntity.ok(alumnoService.listar(activo, pagina, tamano));
    }

    @Operation(summary = "Editar la ficha de un alumno", description = "Solo profesor/admin. Cambiar el correo cambia "
            + "con cuál inicia sesión esa persona. Cambiar el DNI **no** recalcula la contraseña.")
    @ApiResponse(responseCode = "400", description = "El DNI no es válido")
    @ApiResponse(responseCode = "404", description = "El alumno o el grado indicado no existen")
    @ApiResponse(responseCode = "409", description = "Ya hay otra cuenta con ese correo")
    @PutMapping("/{id}")
    public ResponseEntity<AlumnoDto> editar(@PathVariable Long id, @Valid @RequestBody EditarAlumnoRequest request) {
        return ResponseEntity.ok(alumnoService.editar(id, request));
    }

    @Operation(summary = "Descargar la plantilla CSV", description = "Solo profesor/admin. Únicamente la cabecera: "
            + "se rellena a mano o con lo que se quiera y se sube por `POST /api/alumnos/importar`.")
    @GetMapping(value = "/plantilla.csv", produces = "text/csv")
    public ResponseEntity<String> plantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alumnado.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(alumnoService.plantillaCsv());
    }

    @Operation(summary = "Importar alumnado desde el CSV", description = "Solo profesor/admin. Todo o nada: si una "
            + "fila falla no se crea ninguna cuenta y el mensaje dice qué línea y por qué. Las cuentas creadas quedan "
            + "**sin confirmar**, y su contraseña inicial es el DNI sin la letra.")
    @ApiResponse(responseCode = "400", description = "El fichero o alguna de sus filas no es válida (ninguna cuenta creada)")
    @PostMapping("/importar")
    public ResponseEntity<ImportacionDto> importar(@RequestParam("fichero") MultipartFile fichero) {
        return ResponseEntity.ok(alumnoService.importar(fichero));
    }
}
