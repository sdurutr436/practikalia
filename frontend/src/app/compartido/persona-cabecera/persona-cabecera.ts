import { Component, input, output } from '@angular/core';
import { IconoComponent } from '../icono/icono';

/**
 * Cabecera de una tarjeta `.c-persona`: nombre + botón de editar. Repetida
 * igual en alumnado y profesorado; el resto de la tarjeta (clase, pies,
 * acciones) sigue siendo propio de cada dominio, que difiere demasiado
 * entre los dos para forzarlo en un único componente.
 */
@Component({
  selector: 'app-persona-cabecera',
  imports: [IconoComponent],
  templateUrl: './persona-cabecera.html',
})
export class PersonaCabeceraComponent {
  readonly nombre = input.required<string>();
  readonly mostrarEditar = input(true);

  readonly editar = output<void>();
}
