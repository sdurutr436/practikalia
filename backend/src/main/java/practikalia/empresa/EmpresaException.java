package practikalia.empresa;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/** Errores de gestión de empresas de prácticas: catálogo (sector/etiquetas), existencia e imagen. */
public class EmpresaException extends ApiException {

    private EmpresaException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** El `sectorId` indicado o alguno de los `etiquetaIds` no existe como {@code Etiqueta}. */
    public static EmpresaException etiquetaNoEncontrada() {
        return new EmpresaException(HttpStatus.BAD_REQUEST, "ETIQUETA_NO_ENCONTRADA",
                "El sector o alguna de las etiquetas indicadas no existe");
    }

    /** El id de empresa no existe, o (para un alumno) existe pero no está publicada. */
    public static EmpresaException noEncontrada() {
        return new EmpresaException(HttpStatus.NOT_FOUND, "EMPRESA_NO_ENCONTRADA", "La empresa no existe");
    }

    /** El fichero subido no pasa la validación de imagen (firma de bytes JPEG/PNG/WebP o límite de 5 MB); el mensaje detalla el motivo concreto. */
    public static EmpresaException imagenInvalida(String mensaje) {
        return new EmpresaException(HttpStatus.BAD_REQUEST, "IMAGEN_INVALIDA", mensaje);
    }
}
