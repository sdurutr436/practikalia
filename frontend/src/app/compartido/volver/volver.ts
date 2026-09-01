import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconoComponent } from '../icono/icono';

/**
 * Enlace de vuelta de una pantalla de detalle o de formulario. Seis usos, y
 * cuatro de ellos idénticos carácter a carácter.
 */
@Component({
  selector: 'app-volver',
  imports: [RouterLink, IconoComponent],
  templateUrl: './volver.html',
  // .c-volver es ítem de la rejilla de .o-pagina y lleva align-self: start.
  host: { class: 'u-contenidos' },
})
export class VolverComponent {
  readonly a = input.required<string | unknown[]>();
  readonly texto = input('Volver al listado');
}
