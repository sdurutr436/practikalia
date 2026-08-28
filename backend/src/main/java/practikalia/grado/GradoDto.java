package practikalia.grado;

/**
 * Catálogo de grados/ciclos. Solo lectura: el alta/baja sigue gestionándose
 * directamente en base de datos por cada centro, fuera de la app (mismo
 * patrón que {@code Etiqueta}).
 */
public record GradoDto(Long id, String nombre) {

    public static GradoDto de(Grado grado) {
        return new GradoDto(grado.getId(), grado.getNombre());
    }
}
