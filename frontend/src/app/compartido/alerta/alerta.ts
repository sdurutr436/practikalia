import { Component, computed, input } from '@angular/core';

/**
 * Aviso de una línea, en error o en informativo. Existe porque el par
 * clase + `role` se escribía a mano en 24 sitios y tienen que ir emparejados:
 * un `c-alerta--error` con `role="status"` no interrumpe al lector de pantalla
 * cuando debería, y nada avisa del descuadre.
 */
@Component({
  selector: 'app-alerta',
  templateUrl: './alerta.html',
  host: { class: 'u-contenidos' },
})
export class AlertaComponent {
  readonly tipo = input<'error' | 'aviso'>('error');

  protected readonly rol = computed(() => (this.tipo() === 'error' ? 'alert' : 'status'));
}
