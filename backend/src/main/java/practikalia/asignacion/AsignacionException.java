package practikalia.asignacion;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/** Errores de asignación de alumno a empresa: roles inválidos, existencia y duplicados. */
public class AsignacionException extends ApiException {

    private AsignacionException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** El usuario indicado como `alumnoId` existe pero su rol no es `ALUMNO`. */
    public static AsignacionException alumnoInvalido() {
        return new AsignacionException(HttpStatus.BAD_REQUEST, "ALUMNO_INVALIDO", "El usuario indicado no tiene rol de alumno");
    }

    /** El usuario indicado como `tutorCentroId` existe pero su rol no es `PROFESOR`. */
    public static AsignacionException tutorInvalido() {
        return new AsignacionException(HttpStatus.BAD_REQUEST, "TUTOR_INVALIDO", "El usuario indicado no tiene rol de profesor");
    }

    /** El `alumnoId` indicado no existe. */
    public static AsignacionException alumnoNoEncontrado() {
        return new AsignacionException(HttpStatus.NOT_FOUND, "ALUMNO_NO_ENCONTRADO", "El alumno no existe");
    }

    /** El `tutorCentroId` indicado no existe. */
    public static AsignacionException tutorNoEncontrado() {
        return new AsignacionException(HttpStatus.NOT_FOUND, "TUTOR_NO_ENCONTRADO", "El tutor no existe");
    }

    /** Ya existe una asignación de ese alumno a esa empresa para ese mismo grado y año. */
    public static AsignacionException yaExiste() {
        return new AsignacionException(HttpStatus.CONFLICT, "ASIGNACION_YA_EXISTE", "Ya existe una asignación de ese alumno a esa empresa");
    }

    /** El id de asignación indicado no existe. */
    public static AsignacionException noEncontrada() {
        return new AsignacionException(HttpStatus.NOT_FOUND, "ASIGNACION_NO_ENCONTRADA", "La asignación no existe");
    }
}
