package practikalia.common;

import org.springframework.http.HttpStatus;

/** Errores de subida de una imagen: cualquier feature que suba ficheros los reutiliza. */
public class ImagenException extends ApiException {

    private ImagenException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** El fichero no pasa la validación (tamaño, o la firma de bytes no es JPEG/PNG/WebP); el mensaje detalla el motivo. */
    public static ImagenException invalida(String mensaje) {
        return new ImagenException(HttpStatus.BAD_REQUEST, "IMAGEN_INVALIDA", mensaje);
    }
}
