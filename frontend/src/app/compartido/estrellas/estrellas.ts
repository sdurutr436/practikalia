import { Component, computed, input } from '@angular/core';

/**
 * Calificación en estrellas. El máximo es obligatorio y siempre viene de
 * `/api/reviews/calificacion-config`: el rango lo fija cada instituto, así que
 * no se puede dar por hecho que sean cinco.
 *
 * Las estrellas son decoración: el anfitrión lleva `role="img"` y el texto
 * («4 de 5») en `aria-label`, de forma que un lector de pantalla lee la nota
 * una sola vez en vez de deletrear un puñado de símbolos.
 */
@Component({
  selector: 'app-estrellas',
  templateUrl: './estrellas.html',
  host: { class: 'c-estrellas', role: 'img', '[attr.aria-label]': 'texto()' },
})
export class EstrellasComponent {
  readonly valor = input.required<number>();
  readonly maximo = input.required<number>();

  protected readonly texto = computed(() => `${this.valor()} de ${this.maximo()}`);
  protected readonly casillas = computed(() => {
    const llenas = Math.round(this.valor());
    return Array.from({ length: this.maximo() }, (_, indice) => indice < llenas);
  });
}
