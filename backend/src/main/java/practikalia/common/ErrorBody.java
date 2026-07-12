package practikalia.common;

/**
 * Cuerpo JSON de cualquier respuesta de error de la API. {@code codigo} es
 * estable y pensado para que un cliente distinga casos por programación
 * (p. ej. {@code "USUARIO_NO_ENCONTRADO"}); {@code mensaje} es texto legible
 * para mostrar o depurar, no garantizado estable entre versiones.
 */
public record ErrorBody(String codigo, String mensaje) {
}
