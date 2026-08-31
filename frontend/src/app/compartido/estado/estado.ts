import { Component, input } from '@angular/core';

/**
 * Estados de una carga remota: cargando → error → vacío → contenido. Extraído
 * porque el mismo bloque @if/@else if se repetía en todas las pantallas que
 * piden datos, con los mismos textos y el mismo role="alert".
 *
 * El contenido proyectado se instancia siempre (así funciona ng-content), así
 * que solo debe leer señales que ya tengan un valor por defecto seguro —
 * lo cumplen todos los listados, que arrancan en [].
 */
@Component({
  selector: 'app-estado',
  templateUrl: './estado.html',
})
export class EstadoComponent {
  readonly cargando = input(false);
  readonly error = input<string | null>(null);
  readonly vacio = input(false);
  readonly mensajeCargando = input('Cargando…');
  readonly mensajeVacio = input('Todavía no hay nada que mostrar.');
}
