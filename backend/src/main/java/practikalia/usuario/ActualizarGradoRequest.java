package practikalia.usuario;

import jakarta.validation.constraints.NotNull;

public record ActualizarGradoRequest(@NotNull Long gradoId, @NotNull Integer anio) {
}
