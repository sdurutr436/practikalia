import { NgTemplateOutlet } from '@angular/common';
import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { IconoComponent, NombreIcono } from '../icono/icono';

/**
 * Campo de formulario: rótulo con icono, marca de obligatorio, el control
 * proyectado, la ayuda y el error. Es el patrón más repetido de la aplicación
 * —34 copias en nueve pantallas— y en 21 de ellas el bloque de error venía
 * copiado con la misma condición `touched && invalid`, que es justo lo que se
 * olvida al añadir un campo nuevo.
 *
 * El control sigue siendo el nativo de cada pantalla, proyectado con su
 * `formControlName`: aquí no hay `ControlValueAccessor` (decisión de F1).
 */
@Component({
  selector: 'app-campo',
  imports: [NgTemplateOutlet, IconoComponent],
  templateUrl: './campo.html',
  // El ítem de la rejilla de .c-formulario es el .c-campo de dentro.
  host: { class: 'u-contenidos' },
})
export class CampoComponent {
  readonly etiqueta = input.required<string>();
  /** id del control proyectado: alimenta el `for` del rótulo y el id de la ayuda. */
  readonly para = input<string>();
  readonly icono = input<NombreIcono>();
  readonly requerido = input(false);
  readonly ayuda = input<string>();
  /** Control al que mirar para decidir si se pinta `error`. */
  readonly control = input<AbstractControl>();
  readonly error = input<string>();
  /** Grupo de casillas: `<fieldset>`/`<legend>` en vez de `<div>`/`<label>`. */
  readonly grupo = input(false);
  /** Ocupa el hueco sobrante de una fila `.c-acciones`. */
  readonly crecer = input(false);

  // Método y no `computed`: `touched`/`invalid` de un AbstractControl no son
  // señales, así que hay que releerlos en cada ciclo igual que hacía la
  // condición suelta escrita en cada plantilla.
  protected hayError(): boolean {
    const control = this.control();
    return !!this.error() && !!control?.touched && control.invalid;
  }
}
