import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, input, output, signal } from '@angular/core';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';
import { EstrellasComponent } from '../../compartido/estrellas/estrellas';
import { ModalComponent } from '../../compartido/modal/modal';
import { Review } from '../review.model';

/** El mismo mínimo que exige el backend: el alumno lee el motivo para saber qué corregir. */
export const MINIMO_MOTIVO = 20;

/**
 * Rechazo de una reseña, en dos presentaciones sobre el mismo campo:
 *
 * - En ancho de escritorio, dentro de un modal que además recuerda la reseña.
 *   Salir con algo escrito abre un segundo modal de confirmación por encima
 *   —el `<dialog>` nativo los apila solo—, así que no se tira el texto sin avisar.
 * - En una columna (`enLinea`), desplegado dentro de la propia tarjeta. Ahí no
 *   hay confirmación: no existen ni el clic fuera ni Escape, que eran los
 *   accidentes contra los que protegía, y pulsar el botón es deliberado.
 */
@Component({
  selector: 'app-rechazo-modal',
  imports: [NgTemplateOutlet, ModalComponent, BotonComponent, EstrellasComponent, AlertaComponent],
  templateUrl: './rechazo-modal.html',
})
export class RechazoModalComponent {
  readonly review = input.required<Review>();
  readonly maximoEstrellas = input.required<number>();
  readonly enviando = input(false);
  readonly error = input<string | null>(null);
  /** Desplegado dentro de la tarjeta en vez de en un modal. */
  readonly enLinea = input(false);

  readonly confirmar = output<string>();
  readonly cerrar = output<void>();

  protected readonly minimo = MINIMO_MOTIVO;
  protected readonly motivo = signal('');
  protected readonly confirmandoSalida = signal(false);

  protected readonly escrito = computed(() => this.motivo().trim().length);
  protected readonly faltan = computed(() => Math.max(0, MINIMO_MOTIVO - this.escrito()));
  /** Lleva el id de la reseña: en línea puede haber otras tarjetas alrededor. */
  protected readonly idCampo = computed(() => `motivo-rechazo-${this.review().id}`);

  /**
   * En línea se sale directo. En modal, con el campo vacío tampoco hay nada que
   * perder; en cuanto hay texto, cualquier salida pasa por la confirmación.
   */
  protected pedirSalida(): void {
    if (this.enLinea() || this.escrito() === 0) {
      this.cerrar.emit();
      return;
    }
    this.confirmandoSalida.set(true);
  }

  protected seguirEscribiendo(): void {
    this.confirmandoSalida.set(false);
  }

  protected enviar(): void {
    if (this.faltan() > 0 || this.enviando()) {
      return;
    }
    this.confirmar.emit(this.motivo().trim());
  }
}
