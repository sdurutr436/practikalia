import { Component, input, output } from '@angular/core';
import { BotonComponent } from '../boton/boton';
import { ModalComponent } from '../modal/modal';

/**
 * Segundo modal de "¿seguro que quieres salir?" sobre cambios sin guardar —
 * el `<dialog>` nativo apila este por encima del que lo abre. Repetido igual
 * en la ficha de persona y en el rechazo de reseña, solo cambiaba el mensaje
 * y el verbo de "seguir editando/escribiendo".
 */
@Component({
  selector: 'app-confirmar-salida',
  imports: [ModalComponent, BotonComponent],
  templateUrl: './confirmar-salida.html',
})
export class ConfirmarSalidaComponent {
  readonly mensaje = input.required<string>();
  readonly textoSeguir = input('Seguir editando');

  readonly salir = output<void>();
  readonly seguir = output<void>();
}
