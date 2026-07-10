package practikalia.empresa;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
        boolean publicada) {
}
