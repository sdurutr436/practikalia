import { Component, computed, input, output } from '@angular/core';
import { IconoComponent } from '../icono/icono';

/**
 * Aviso de una línea, en error o en informativo. Existe porque el par
 * clase + `role` se escribía a mano en 24 sitios y tienen que ir emparejados:
 * un `c-alerta--error` con `role="status"` no interrumpe al lector de pantalla
 * cuando debería, y nada avisa del descuadre.
 */
@Component({
  selector: 'app-alerta',
  imports: [IconoComponent],
  templateUrl: './alerta.html',
  host: { class: 'u-contenidos' },
})
export class AlertaComponent {
  readonly tipo = input<'error' | 'aviso'>('error');
  /** Añade la X. Para los avisos que no los quita ninguna otra acción de la pantalla. */
  readonly cerrable = input(false);
  readonly cerrar = output<void>();

  protected readonly rol = computed(() => (this.tipo() === 'error' ? 'alert' : 'status'));
}
