package practikalia.asignacion;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

public class AsignacionException extends ApiException {

    private AsignacionException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static AsignacionException alumnoInvalido() {
        return new AsignacionException(HttpStatus.BAD_REQUEST, "ALUMNO_INVALIDO", "El usuario indicado no tiene rol de alumno");
    }

    public static AsignacionException tutorInvalido() {
        return new AsignacionException(HttpStatus.BAD_REQUEST, "TUTOR_INVALIDO", "El usuario indicado no tiene rol de profesor");
    }

    public static AsignacionException alumnoNoEncontrado() {
        return new AsignacionException(HttpStatus.NOT_FOUND, "ALUMNO_NO_ENCONTRADO", "El alumno no existe");
    }

    public static AsignacionException tutorNoEncontrado() {
        return new AsignacionException(HttpStatus.NOT_FOUND, "TUTOR_NO_ENCONTRADO", "El tutor no existe");
    }

    public static AsignacionException yaExiste() {
        return new AsignacionException(HttpStatus.CONFLICT, "ASIGNACION_YA_EXISTE", "Ya existe una asignación de ese alumno a esa empresa");
    }

    public static AsignacionException noEncontrada() {
        return new AsignacionException(HttpStatus.NOT_FOUND, "ASIGNACION_NO_ENCONTRADA", "La asignación no existe");
    }
}
