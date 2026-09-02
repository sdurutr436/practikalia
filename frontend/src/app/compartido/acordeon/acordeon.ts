import { Component, input, model } from '@angular/core';
import { IconoComponent } from '../icono/icono';

let contador = 0;

/**
 * Sección plegable: un botón que abre y cierra lo que se le proyecte. El
 * estado es un `model`, así que quien lo usa puede dejarlo a su aire o
 * compartirlo con otro control — la barra lo comparte con su hamburguesa, que
 * abre lo mismo en pantallas anchas.
 */
@Component({
  selector: 'app-acordeon',
  imports: [IconoComponent],
  templateUrl: './acordeon.html',
  host: { class: 'c-acordeon' },
})
export class AcordeonComponent {
  readonly etiqueta = input.required<string>();
  readonly abierto = model(false);

  /** `aria-controls` necesita un id propio aunque haya varios en la página. */
  protected readonly id = `acordeon-${++contador}`;
}
