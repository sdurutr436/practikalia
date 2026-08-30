import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Empresa } from '../empresa.model';

/**
 * Tarjeta de empresa del listado y del listado de afinidad: imagen, nombre y
 * una línea de meta que decide la pantalla (sector, o sector + score). Lo que
 * cuelga debajo (badge de no publicada, etiquetas coincidentes) va proyectado.
 */
@Component({
  selector: 'app-tarjeta-empresa',
  imports: [RouterLink],
  templateUrl: './tarjeta-empresa.html',
  // La tarjeta es un ítem de .o-rejilla: el host no puede quedarse inline.
  host: { class: 'u-bloque' },
})
export class TarjetaEmpresaComponent {
  readonly empresa = input.required<Empresa>();
  readonly meta = input.required<string>();
}
