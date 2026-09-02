package practikalia.empresa;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Criterios de búsqueda del listado de empresas. Todos opcionales: sin
 * ninguno, el listado es el catálogo entero que le corresponda al rol.
 *
 * @param texto      busca en nombre, descripción, sector y etiquetas
 * @param publicada  filtra por estado de publicación (el alumnado siempre ve solo publicadas)
 * @param sectorId   restringe a un sector concreto
 * @param etiquetaIds empresas que tengan **alguna** de esas etiquetas
 * @param pagina     índice de página empezando en 0
 * @param tamano     elementos por página; sin él, todo en una sola página
 */
public record EmpresaFiltroDto(
        String texto,
        Boolean publicada,
        Long sectorId,
        List<Long> etiquetaIds,
        int pagina,
        @Schema(description = "Elementos por página. Omitido, devuelve el listado entero en una página")
        Integer tamano) {
}
