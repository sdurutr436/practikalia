import { Component, ElementRef, computed, input, output, signal, viewChild } from '@angular/core';
import { IconoComponent } from '../icono/icono';

/** Lo que se espera a que pare de teclear; cada búsqueda es una consulta. */
const ESPERA_TECLEO = 250;

/**
 * Cómo se planta la barra en la fila de filtros:
 * - `desplegable`: cerrada mide lo que su lupa y se estira al pulsarla;
 * - `fijo`: siempre abierta, la variante que sale en pantalla estrecha;
 * - `ancho`: siempre abierta y ocupando la fila entera.
 */
export type ModoBuscador = 'desplegable' | 'fijo' | 'ancho';

/**
 * Barra de búsqueda de un listado. Se queda con lo que se repetía en cada
 * pantalla que busca: la espera antes de consultar, el foco al desplegarse y
 * que cerrarla limpia la búsqueda. Emite ya el texto listo para llevarlo a la
 * URL; qué se hace con él lo decide cada pantalla.
 *
 * Lo que se proyecte dentro sale al final de la barra, que es donde el listado
 * de empresas mete su botón de filtros.
 */
@Component({
  selector: 'app-buscador',
  imports: [IconoComponent],
  templateUrl: './buscador.html',
  host: { class: 'u-contenidos' },
})
export class BuscadorComponent {
  /** Texto de partida, normalmente el que trae la URL. */
  readonly valor = input('');
  /** Rótulo para el lector de pantalla: «Buscar empresas». */
  readonly etiqueta = input.required<string>();
  readonly marcador = input('');
  readonly modo = input<ModoBuscador>('ancho');
  readonly buscar = output<string>();

  protected readonly abierto = signal(false);
  /** Los modos que no se pliegan nacen abiertos y no se cierran nunca. */
  protected readonly desplegado = computed(() => this.modo() !== 'desplegable' || this.abierto());

  private readonly entrada = viewChild.required<ElementRef<HTMLInputElement>>('entrada');
  private temporizador?: ReturnType<typeof setTimeout>;

  protected escribir(valor: string): void {
    clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => this.buscar.emit(valor), ESPERA_TECLEO);
  }

  /** La lupa despliega el campo y le da el foco; cerrarla limpia la búsqueda. */
  protected alternar(): void {
    if (this.abierto()) {
      this.abierto.set(false);
      clearTimeout(this.temporizador);
      this.buscar.emit('');
      return;
    }
    this.abierto.set(true);
    this.entrada().nativeElement.focus();
  }

  protected cerrarConEscape(): void {
    if (this.modo() === 'desplegable' && this.abierto()) {
      this.alternar();
    }
  }
}
