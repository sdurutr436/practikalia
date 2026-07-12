package practikalia.usuario;

import jakarta.validation.constraints.NotNull;

/** Petición de reemplazo del perfil de grado/año de un usuario (siempre ambos campos a la vez). */
public record ActualizarGradoRequest(@NotNull Long gradoId, @NotNull Integer anio) {
}
