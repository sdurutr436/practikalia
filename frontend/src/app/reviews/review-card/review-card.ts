import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstrellasComponent } from '../../compartido/estrellas/estrellas';
import { Review } from '../review.model';

/**
 * Tarjeta de una reseña: cabecera (empresa + estrellas), autor, texto y
 * motivo de rechazo si lo hay. Vivía copiada en el panel, la cola de
 * moderación y la ficha de empresa, cada una con su propio detalle suelto
 * (una sin estrellas, otra sin el nombre del alumno...). Las acciones
 * (aprobar/rechazar/editar…) varían según la pantalla, así que se proyectan
 * con `<ng-content>` en vez de vivir aquí.
 */
@Component({
  selector: 'app-review-card',
  imports: [RouterLink, EstrellasComponent],
  templateUrl: './review-card.html',
  host: { class: 'u-contenidos' },
})
export class ReviewCardComponent {
  readonly review = input.required<Review>();
  /** Sin ella no se pintan estrellas: el rango de calificación lo fija cada instituto. */
  readonly maximoEstrellas = input<number | null>(null);
  /** La ficha de la propia empresa no necesita enlazar a sí misma. */
  readonly mostrarEmpresa = input(true);
  /** Solo hace falta cuando la lista mezcla reviews de varios estados a la vez. */
  readonly mostrarEstado = input(false);
}
