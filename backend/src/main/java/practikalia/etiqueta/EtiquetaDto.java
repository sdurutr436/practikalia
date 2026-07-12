package practikalia.etiqueta;

/**
 * Catálogo de etiquetas (usado tanto como sector de empresa como como interés
 * de alumno/empresa). Sin controller propio: se gestiona directamente en
 * base de datos por cada centro, fuera de la app; solo se lee, anidado en
 * otras respuestas (empresa, perfil de usuario, afinidad).
 */
public record EtiquetaDto(Long id, String nombre) {

    public static EtiquetaDto de(Etiqueta etiqueta) {
        return new EtiquetaDto(etiqueta.getId(), etiqueta.getNombre());
    }
}
