package practikalia.review;

import practikalia.common.ApiException;

import org.springframework.http.HttpStatus;

public class ReviewException extends ApiException {

    private ReviewException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static ReviewException campoInvalido(String mensaje) {
        return new ReviewException(HttpStatus.BAD_REQUEST, "CAMPO_INVALIDO", mensaje);
    }

    public static ReviewException noEncontrada() {
        return new ReviewException(HttpStatus.NOT_FOUND, "REVIEW_NO_ENCONTRADA", "La review no existe");
    }

    public static ReviewException yaExiste() {
        return new ReviewException(HttpStatus.CONFLICT, "REVIEW_YA_EXISTE", "El alumno ya tiene una review sobre esa empresa");
    }

    public static ReviewException yaModerada() {
        return new ReviewException(HttpStatus.CONFLICT, "REVIEW_YA_MODERADA", "La review ya ha sido moderada");
    }
}
