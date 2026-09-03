package practikalia.empresa.tutor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Un tutor de empresa, tal y como viaja en la ficha de su empresa: se lee y se
 * escribe con la misma forma. Al guardar, {@code id} nulo es un tutor nuevo, y
 * los que falten en la lista se borran.
 */
public record TutorEmpresaDto(
        @Schema(description = "`null` al crearlo desde la ficha")
        Long id,
        @NotBlank String nombre,
        @Schema(description = "Su puesto en la empresa, para saber a quién toca cada alumno")
        String cargo,
        String telefono,
        String correo) {

    public static TutorEmpresaDto de(TutorEmpresa tutor) {
        return new TutorEmpresaDto(
                tutor.getId(), tutor.getNombre(), tutor.getCargo(), tutor.getTelefono(), tutor.getCorreo());
    }
}
