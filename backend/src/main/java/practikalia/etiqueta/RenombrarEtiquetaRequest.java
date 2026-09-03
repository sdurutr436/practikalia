package practikalia.etiqueta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo único editable de un nodo: mover de rama no está previsto todavía. */
public record RenombrarEtiquetaRequest(@NotBlank @Size(max = 255) String nombre) {
}
