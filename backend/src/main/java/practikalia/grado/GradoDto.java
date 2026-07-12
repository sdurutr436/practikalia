package practikalia.grado;

/**
 * Catálogo de grados/ciclos. Sin controller propio: se gestiona directamente
 * en base de datos por cada centro, fuera de la app (mismo patrón que
 * {@code Etiqueta}); solo se lee, anidado en otras respuestas
 * (perfil de usuario, empresa, etc.).
 */
public record GradoDto(Long id, String nombre) {

    public static GradoDto de(Grado grado) {
        return new GradoDto(grado.getId(), grado.getNombre());
    }
}
