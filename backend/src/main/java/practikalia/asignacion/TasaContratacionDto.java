package practikalia.asignacion;

import io.swagger.v3.oas.annotations.media.Schema;

/** Tasa de contratación posterior de una empresa, calculada al vuelo (sin caché). */
public record TasaContratacionDto(
        Long empresaId,
        @Schema(description = "Asignaciones cerradas (`fechaFin` no nula) con dato de contratación posterior informado; las abiertas o sin decidir no cuentan")
        long asignacionesDecididas,
        long contrataciones,
        @Schema(description = "`contrataciones / asignacionesDecididas`, entre `0.0` y `1.0`; `0.0` si no hay ninguna decidida")
        double tasa) {
}
