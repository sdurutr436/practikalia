package practikalia.centro;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lo público de la configuración del centro: nunca la whitelist de correos,
 * que es dato personal y no viaja aquí.
 */
public record CentroDto(
        String nombre,
        @Schema(description = "Ruta relativa servida por nginx en `/uploads/`; `null` si el centro no tiene logo todavía")
        String logo) {

    static CentroDto de(Centro centro) {
        return new CentroDto(centro.getNombre(), centro.getLogo());
    }
}
