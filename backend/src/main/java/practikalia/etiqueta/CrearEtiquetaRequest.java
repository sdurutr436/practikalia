package practikalia.etiqueta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de un nodo del catálogo. {@code padreId} decide el nivel: sin padre es
 * un sector (o un grupo transversal si {@code transversal}), con un sector de
 * padre es una actividad y con una actividad de padre es una etiqueta.
 *
 * {@code transversal} es {@code Boolean} y no {@code boolean} porque el campo
 * es opcional: omitirlo en el JSON contra un primitivo revienta la
 * deserialización con un 500 en vez de valer false.
 */
public record CrearEtiquetaRequest(
        @NotBlank @Size(max = 255) String nombre,
        Long padreId,
        Boolean transversal) {
}
