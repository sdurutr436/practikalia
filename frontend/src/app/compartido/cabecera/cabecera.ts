import { Component, input } from '@angular/core';

/**
 * Cabecera de pantalla: el `<h1>` y, proyectadas a su derecha, las acciones
 * que tenga esa pantalla. Ocho de las nueve cabeceras de la aplicación son
 * este mismo bloque con otro texto.
 */
@Component({
  selector: 'app-cabecera',
  templateUrl: './cabecera.html',
  host: { class: 'u-contenidos' },
})
export class CabeceraComponent {
  readonly titulo = input.required<string>();
}
