package practikalia.etiqueta;

import java.util.List;

/**
 * Una rama del catálogo, con sus hijas dentro. El árbol entero son ~25 filas,
 * así que la pantalla de mantenimiento se lo lleva de una sola vez y navega las
 * tres columnas sin volver a preguntar.
 */
public record NodoDto(Long id, String nombre, boolean transversal, List<NodoDto> hijas) {
}
