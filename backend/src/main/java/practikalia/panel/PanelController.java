package practikalia.panel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Resumen del centro para el panel de profesorado: contadores de solo lectura
 * sobre datos que ya existen (empresas y alumnado), sin entidad ni tabla
 * propias.
 */
@RestController
public class PanelController {

    private final PanelService panelService;

    public PanelController(PanelService panelService) {
        this.panelService = panelService;
    }

    @Operation(summary = "Contadores del centro", description = "Solo profesor/admin: el resumen es del centro entero "
            + "e incluye las empresas sin publicar, que el alumnado no ve. Se cuenta al vuelo en cada petición.")
    @ApiResponse(responseCode = "403", description = "Quien pregunta es alumno")
    @GetMapping("/api/panel/resumen")
    public ResponseEntity<ResumenCentroDto> resumen() {
        return ResponseEntity.ok(panelService.resumenCentro());
    }
}
