import { Component, ElementRef, afterNextRender, input, output, viewChild } from '@angular/core';
import { IconoComponent } from '../icono/icono';

/**
 * Modal del tema sobre `<dialog>` nativo. El navegador ya trae foco atrapado,
 * cierre con Escape, fondo inerte y apilado en la top layer: de ahí que un
 * modal encima de otro funcione sin tocar la escala de z-index ni añadir
 * ninguna librería.
 *
 * Nunca se cierra solo. Las tres salidas —la X, Escape y el clic en el fondo—
 * emiten `salir` y es el padre quien decide qué hacer, que es lo que permite
 * pedir confirmación antes de tirar lo que alguien llevaba escrito.
 */
@Component({
  selector: 'app-modal',
  imports: [IconoComponent],
  templateUrl: './modal.html',
  host: { class: 'u-contenidos' },
})
export class ModalComponent {
  readonly titulo = input.required<string>();
  /** Se ha pedido salir (X, Escape o clic en el fondo). Cerrar es cosa del padre. */
  readonly salir = output<void>();

  private readonly dialogo = viewChild.required<ElementRef<HTMLDialogElement>>('dialogo');

  constructor() {
    afterNextRender(() => this.dialogo().nativeElement.showModal());
  }

  /** Escape dispara `cancel`, que cerraría el diálogo por su cuenta. */
  protected alCancelar(evento: Event): void {
    evento.preventDefault();
    this.salir.emit();
  }

  /** Un clic en el fondo llega con el propio `<dialog>` como destino, no con la caja. */
  protected alPulsar(evento: MouseEvent): void {
    if (evento.target === this.dialogo().nativeElement) {
      this.salir.emit();
    }
  }
}
