package practikalia.usuario.alumnado;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Los cursos que ofrece el selector de la pantalla de asignaciones. */
public record CursosDto(
        @Schema(description = "El curso académico en marcha, que es el que se lista si no se pide otro")
        int actual,
        @Schema(description = "Cursos con alumnado matriculado, de más nuevo a más viejo; el actual siempre está")
        List<Integer> cursos) {
}
