package practikalia.panel;

import io.swagger.v3.oas.annotations.media.Schema;

/** Contadores del centro para el panel de profesorado, contados al vuelo (sin caché). */
public record ResumenCentroDto(
        long empresasPublicadas,
        long empresasSinPublicar,
        @Schema(description = "Usuarios con `rol = ALUMNO` y `activo = true`")
        long alumnadoActivo,
        @Schema(description = "Alumnado activo sin ninguna asignación abierta (`fechaFin` nula); uno cuya asignación ya se cerró vuelve a contar aquí, porque está libre para otra")
        long alumnadoSinAsignar) {
}
