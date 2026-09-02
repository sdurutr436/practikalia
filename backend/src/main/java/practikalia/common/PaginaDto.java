package practikalia.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Una página de resultados de un listado. Propia y no el {@code Page} de
 * Spring Data, cuyo JSON no es contrato estable.
 *
 * @param <T> el DTO de cada elemento
 */
public record PaginaDto<T>(
        List<T> contenido,
        @Schema(description = "Índice de la página devuelta, empezando en 0")
        int pagina,
        int tamano,
        @Schema(description = "Elementos que hay en total, no solo en esta página")
        long total,
        int paginas) {

    public static <E, D> PaginaDto<D> de(Page<E> pagina, Function<E, D> aDto) {
        return new PaginaDto<>(
                pagina.getContent().stream().map(aDto).toList(),
                pagina.getNumber(),
                pagina.getNumberOfElements(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
