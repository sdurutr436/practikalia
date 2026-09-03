package practikalia.usuario.profesorado;

import practikalia.common.PaginaDto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * Listado de profesorado del centro y su ficha, con la tutoría de clase y la de
 * prácticas. Leerlo es de cualquier profesor; darlo de alta y editarlo, solo del
 * admin ({@code SecurityConfig}): el profesorado no se edita entre sí.
 */
@RestController
@RequestMapping("/api/profesores")
public class ProfesorController {

    private final ProfesorService profesorService;

    public ProfesorController(ProfesorService profesorService) {
        this.profesorService = profesorService;
    }

    @Operation(summary = "Listar profesorado", description = "Solo profesor/admin. Paginado y ordenado por apellido. "
            + "`conClase` filtra las pastillas de la pantalla: `true` los que tutorizan una clase, `false` los que no, "
            + "sin el parámetro salen todos.")
    @GetMapping
    public ResponseEntity<PaginaDto<ProfesorDto>> listar(
            @RequestParam(required = false) Boolean conClase,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int tamano) {
        return ResponseEntity.ok(profesorService.listar(conClase, pagina, tamano));
    }

    @Operation(summary = "Dar de alta un profesor", description = "Solo admin. Misma ficha que la edición. "
            + "Nace confirmada y su contraseña inicial es el DNI sin la letra. El correo queda añadido a la "
            + "whitelist del centro.")
    @ApiResponse(responseCode = "400", description = "El DNI no es válido, o el dominio del correo no está permitido")
    @ApiResponse(responseCode = "404", description = "La clase indicada no existe, o algún alumno indicado no tiene asignación abierta")
    @ApiResponse(responseCode = "409", description = "Ya hay una cuenta con ese correo o con ese DNI")
    @PostMapping
    public ResponseEntity<ProfesorDto> crear(@Valid @RequestBody FichaProfesorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profesorService.crear(request));
    }

    @Operation(summary = "Editar la ficha de un profesor", description = "Solo admin. Cambiar el correo cambia con "
            + "cuál inicia sesión esa persona. Cambiar el DNI **no** recalcula la contraseña. La clase que tutoriza "
            + "se la quita a quien la tuviera, y los alumnos indicados pasan a tenerle como tutor de prácticas.")
    @ApiResponse(responseCode = "400", description = "El DNI no es válido")
    @ApiResponse(responseCode = "404", description = "El profesor, la clase o la asignación de algún alumno no existen")
    @ApiResponse(responseCode = "409", description = "Ya hay otra cuenta con ese correo, o es el último administrador y se le quita el permiso")
    @PutMapping("/{id}")
    public ResponseEntity<ProfesorDto> editar(@PathVariable Long id, @Valid @RequestBody FichaProfesorRequest request) {
        return ResponseEntity.ok(profesorService.editar(id, request));
    }
}
