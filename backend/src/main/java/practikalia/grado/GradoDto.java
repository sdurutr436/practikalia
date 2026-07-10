package practikalia.grado;

public record GradoDto(Long id, String nombre) {

    public static GradoDto de(Grado grado) {
        return new GradoDto(grado.getId(), grado.getNombre());
    }
}
