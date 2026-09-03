package practikalia.etiqueta;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/** Errores de mantenimiento del catálogo de sectores y etiquetas. */
public class EtiquetaException extends ApiException {

    private EtiquetaException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static EtiquetaException noEncontrada() {
        return new EtiquetaException(HttpStatus.NOT_FOUND, "ETIQUETA_NO_ENCONTRADA",
                "El sector o la etiqueta no existe");
    }

    /** El nombre ya está en el catálogo: es único en toda la tabla, no por rama. */
    public static EtiquetaException nombreRepetido() {
        return new EtiquetaException(HttpStatus.CONFLICT, "ETIQUETA_REPETIDA",
                "Ya existe un sector o una etiqueta con ese nombre");
    }

    /** Colgar de una etiqueta de nivel 3 daría un cuarto nivel que la pantalla no pinta. */
    public static EtiquetaException nivelMaximo() {
        return new EtiquetaException(HttpStatus.BAD_REQUEST, "NIVEL_MAXIMO",
                "El catálogo llega hasta etiqueta: sector, actividad y etiqueta");
    }

    /** Se vacía de abajo arriba: primero las etiquetas, luego la actividad, luego el sector. */
    public static EtiquetaException conHijas() {
        return new EtiquetaException(HttpStatus.CONFLICT, "ETIQUETA_CON_HIJAS",
                "Todavía cuelgan actividades o etiquetas de aquí; vacíalo antes de borrarlo");
    }

    public static EtiquetaException enUso() {
        return new EtiquetaException(HttpStatus.CONFLICT, "ETIQUETA_EN_USO",
                "Alguna empresa o algún alumno la está usando");
    }
}
