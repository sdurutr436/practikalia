import { Component, inject } from '@angular/core';
import { IconoComponent } from '../icono/icono';
import { ToastService } from './toast.service';

/**
 * El aviso de «hecho», abajo a la derecha. Va una sola vez en el marco de la
 * aplicación y lo alimenta {@link ToastService} desde cualquier pantalla.
 *
 * `role="status"` y no `alert`: confirma algo que la persona acaba de pedir,
 * así que el lector de pantalla lo cuenta cuando termine lo que esté diciendo,
 * sin interrumpir.
 */
@Component({
  selector: 'app-toast',
  imports: [IconoComponent],
  templateUrl: './toast.html',
})
export class ToastComponent {
  protected readonly toast = inject(ToastService);
}
