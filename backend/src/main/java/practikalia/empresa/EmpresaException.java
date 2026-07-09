package practikalia.empresa;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

public class EmpresaException extends ApiException {

    private EmpresaException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static EmpresaException etiquetaNoEncontrada() {
        return new EmpresaException(HttpStatus.BAD_REQUEST, "ETIQUETA_NO_ENCONTRADA",
                "El sector o alguna de las etiquetas indicadas no existe");
    }

    public static EmpresaException noEncontrada() {
        return new EmpresaException(HttpStatus.NOT_FOUND, "EMPRESA_NO_ENCONTRADA", "La empresa no existe");
    }

    public static EmpresaException imagenInvalida(String mensaje) {
        return new EmpresaException(HttpStatus.BAD_REQUEST, "IMAGEN_INVALIDA", mensaje);
    }
}
