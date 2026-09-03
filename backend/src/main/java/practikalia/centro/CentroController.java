package practikalia.centro;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
 * Configuración del centro: nombre y logo. Practikalia se despliega por
 * instituto, así que es una fila única, sin alta ni baja.
 */
@RestController
@RequestMapping("/api/centro")
public class CentroController {

    private final CentroService centroService;

    public CentroController(CentroService centroService) {
        this.centroService = centroService;
    }

    @Operation(summary = "Consultar el centro", description = "Sin sesión: la pantalla de acceso pinta el nombre y "
            + "el logo antes de que nadie haya entrado. Nunca devuelve la whitelist de correos.")
    @GetMapping
    public ResponseEntity<CentroDto> obtener() {
        return ResponseEntity.ok(centroService.obtener());
    }

    @Operation(summary = "Renombrar el centro", description = "Solo admin.")
    @PutMapping
    public ResponseEntity<CentroDto> actualizar(@Valid @RequestBody ActualizarCentroRequest request) {
        return ResponseEntity.ok(centroService.actualizar(request));
    }

    @Operation(summary = "Subir el logo del centro", description = "Solo admin. Valida el formato por la firma real "
            + "de bytes del fichero (JPEG/PNG/WebP), no por extensión ni Content-Type declarado; máximo 5 MB.")
    @ApiResponse(responseCode = "400", description = "El fichero no es una imagen JPEG/PNG/WebP válida, o supera los 5 MB")
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CentroDto> subirLogo(@RequestParam("fichero") MultipartFile fichero) {
        return ResponseEntity.ok(centroService.actualizarLogo(fichero));
    }
}
