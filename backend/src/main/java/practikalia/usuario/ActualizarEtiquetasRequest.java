package practikalia.usuario;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Petición de reemplazo completo de las etiquetas de interés de un usuario. */
public record ActualizarEtiquetasRequest(
        @Schema(description = "Lista completa nueva de ids de etiqueta; `[]` o ausente vacía la lista")
        List<Long> etiquetaIds) {
}
