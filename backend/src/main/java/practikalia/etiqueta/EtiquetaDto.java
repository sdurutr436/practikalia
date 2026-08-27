package practikalia.etiqueta;

/**
 * Catálogo de etiquetas (usado tanto como sector de empresa como como interés
 * de alumno/empresa). El alta/baja se gestiona directamente en base de datos
 * por cada centro, fuera de la app; la lectura tiene tanto
 * {@link EtiquetaController} (listado propio, {@code GET /api/etiquetas})
 * como su forma anidada en otras respuestas (empresa, perfil de usuario,
 * afinidad).
 */
public record EtiquetaDto(Long id, String nombre) {

    public static EtiquetaDto de(Etiqueta etiqueta) {
        return new EtiquetaDto(etiqueta.getId(), etiqueta.getNombre());
    }
}
