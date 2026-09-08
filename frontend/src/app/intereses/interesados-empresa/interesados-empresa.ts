import { Component, input } from '@angular/core';
import { EstadoComponent } from '../../compartido/estado/estado';
import { Interesado } from '../interes.model';

/** Lista de alumnado interesado en una empresa, vista de profesorado. */
@Component({
  selector: 'app-interesados-empresa',
  imports: [EstadoComponent],
  templateUrl: './interesados-empresa.html',
})
export class InteresadosEmpresaComponent {
  readonly interesados = input.required<Interesado[]>();
  readonly cargando = input(false);
  readonly error = input<string | null>(null);
}
