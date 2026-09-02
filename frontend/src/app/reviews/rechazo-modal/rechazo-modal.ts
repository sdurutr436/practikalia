import { Component, computed, input, output, signal } from '@angular/core';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';
import { EstrellasComponent } from '../../compartido/estrellas/estrellas';
import { ModalComponent } from '../../compartido/modal/modal';
import { Review } from '../review.model';

/** El mismo mínimo que exige el backend: el alumno lee el motivo para saber qué corregir. */
export const MINIMO_MOTIVO = 20;

/**
 * Rechazo de una reseña: enseña la reseña que se va a rechazar y pide el
 * motivo. Salir con algo escrito abre un segundo modal de confirmación por
 * encima —el `<dialog>` nativo los apila solo—, así que ningún camino de salida
 * tira el texto sin avisar.
 */
@Component({
  selector: 'app-rechazo-modal',
  imports: [ModalComponent, BotonComponent, EstrellasComponent, AlertaComponent],
  templateUrl: './rechazo-modal.html',
})
export class RechazoModalComponent {
  readonly review = input.required<Review>();
  readonly maximoEstrellas = input.required<number>();
  readonly enviando = input(false);
  readonly error = input<string | null>(null);

  readonly confirmar = output<string>();
  readonly cerrar = output<void>();

  protected readonly minimo = MINIMO_MOTIVO;
  protected readonly motivo = signal('');
  protected readonly confirmandoSalida = signal(false);

  protected readonly escrito = computed(() => this.motivo().trim().length);
  protected readonly faltan = computed(() => Math.max(0, MINIMO_MOTIVO - this.escrito()));

  /**
   * Con el campo vacío no hay nada que perder y salir es directo; en cuanto hay
   * texto, cualquier salida pasa por la confirmación.
   */
  protected pedirSalida(): void {
    if (this.escrito() === 0) {
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
