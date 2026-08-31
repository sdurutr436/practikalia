import { Component, ElementRef, input, viewChild } from '@angular/core';
import { IconoComponent } from '../icono/icono';

/**
 * Tira horizontal con desplazamiento por pasos. El deslizamiento y el imán de
 * parada los hace el propio navegador (`overflow-x` + `scroll-snap`): los
 * botones solo llaman a `scrollBy`, así que no hay estado de "página actual"
 * que mantener ni dependencia de carrusel que instalar.
 *
 * Lo que se le proyecte son los ítems; el ancho de cada uno lo fija
 * `.c-carrusel__pista` en la hoja de estilos.
 */
@Component({
  selector: 'app-carrusel',
  imports: [IconoComponent],
  templateUrl: './carrusel.html',
  host: { class: 'c-carrusel' },
})
export class CarruselComponent {
  private readonly pista = viewChild.required<ElementRef<HTMLElement>>('pista');

  /** Para las etiquetas de los botones: "Ver más empresas", "empresas anteriores". */
  readonly etiqueta = input('elementos');

  protected desplazar(signo: 1 | -1): void {
    const pista = this.pista().nativeElement;
    // Un poco menos de un ancho visible, para que el ítem del borde no se pierda.
    pista.scrollBy({ left: signo * pista.clientWidth * 0.8, behavior: 'smooth' });
  }
}
