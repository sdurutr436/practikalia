package practikalia.review;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

/** Errores de reviews: validación de campos, existencia, duplicados y moderación. */
public class ReviewException extends ApiException {

    private ReviewException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    /** Calificación fuera de rango al crear/editar, o estado/motivo inválidos al moderar; el mensaje detalla el motivo concreto. */
    public static ReviewException campoInvalido(String mensaje) {
        return new ReviewException(HttpStatus.BAD_REQUEST, "CAMPO_INVALIDO", mensaje);
    }

    /** El id de review indicado no existe. */
    public static ReviewException noEncontrada() {
        return new ReviewException(HttpStatus.NOT_FOUND, "REVIEW_NO_ENCONTRADA", "La review no existe");
    }

    /** La asignación indicada al crear ya tiene una review (una por asignación). */
    public static ReviewException yaExiste() {
        return new ReviewException(HttpStatus.CONFLICT, "REVIEW_YA_EXISTE", "El alumno ya tiene una review sobre esa empresa");
    }

    /** Se intenta moderar una review que ya no está en `PENDIENTE`. */
    public static ReviewException yaModerada() {
        return new ReviewException(HttpStatus.CONFLICT, "REVIEW_YA_MODERADA", "La review ya ha sido moderada");
    }
}
