import { Component, input, output } from '@angular/core';
import { BotonComponent } from '../boton/boton';

/**
 * Paginación de un listado: anterior, la posición y siguiente. Estaba copiada
 * tal cual en empresas, reseñas, alumnado y asignaciones —el mismo `<nav>`, los
 * mismos dos botones y las mismas dos condiciones de borde—, y esas condiciones
 * son justo lo que se olvida al añadir la quinta pantalla.
 *
 * No navega: emite la página a la que se va y cada pantalla decide cómo lo
 * escribe en su URL.
 */
@Component({
  selector: 'app-paginacion',
  imports: [BotonComponent],
  templateUrl: './paginacion.html',
  host: { class: 'u-contenidos' },
})
export class PaginacionComponent {
  /** Página actual, empezando en 0. */
  readonly pagina = input.required<number>();
  readonly paginas = input.required<number>();
  /** De qué son las páginas, para el rótulo del `<nav>`: «Páginas de reseñas». */
  readonly seccion = input.required<string>();
  /** Elementos en total; sin él la posición no lleva contador. */
  readonly total = input<number | null>(null);
  /** Plural de lo que se cuenta: «alumnos», «reseñas»… */
  readonly nombre = input('');
  readonly ir = output<number>();
}
