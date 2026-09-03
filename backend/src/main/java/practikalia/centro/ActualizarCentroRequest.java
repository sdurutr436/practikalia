package practikalia.centro;

import jakarta.validation.constraints.NotBlank;

/** El logo se sube aparte (`POST /api/centro/logo`, multipart); aquí solo el nombre. */
public record ActualizarCentroRequest(@NotBlank String nombre) {
}
