package practikalia.empresa;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Petición de creación/actualización completa de una empresa. Se reutiliza
 * para ambas operaciones: en creación, {@code publicada} se ignora (nace
 * siempre en falso); en actualización, sí se honra.
 */
public record CrearEmpresaRequest(
        @NotBlank String nombre,
        String descripcion,
        String direccion,
        @NotNull Long sectorId,
        List<Long> etiquetaIds,
        String observaciones,
        String contactoNombre,
        String contactoTelefono,
        String contactoEmail,
        @Schema(description = "Ignorado al crear (nace `false`); honrado al actualizar")
        boolean publicada) {
}
