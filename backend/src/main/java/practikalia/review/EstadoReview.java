package practikalia.review;

/** Ciclo de moderación de una review: nace `PENDIENTE` (o `APROBADA` si la crea el tutor) y solo se modera una vez desde `PENDIENTE`. */
public enum EstadoReview {
    PENDIENTE,
    APROBADA,
    RECHAZADA
}
