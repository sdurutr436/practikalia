import { Component, input, output } from '@angular/core';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';

/** Botón de marcar/quitar interés de un alumno en una empresa. */
@Component({
  selector: 'app-interes-boton',
  imports: [AlertaComponent, BotonComponent],
  templateUrl: './interes-boton.html',
})
export class InteresBotonComponent {
  readonly interesado = input(false);
  readonly guardando = input(false);
  readonly error = input<string | null>(null);

  readonly alternar = output<void>();
}
