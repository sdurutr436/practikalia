package practikalia.etiqueta;

public record EtiquetaDto(Long id, String nombre) {

    public static EtiquetaDto de(Etiqueta etiqueta) {
        return new EtiquetaDto(etiqueta.getId(), etiqueta.getNombre());
    }
}
