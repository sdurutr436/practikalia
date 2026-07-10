package practikalia.asignacion;

public record TasaContratacionDto(
        Long empresaId,
        long asignacionesDecididas,
        long contrataciones,
        double tasa) {
}
