import { NgTemplateOutlet } from '@angular/common';
import { Component, input, linkedSignal, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

/** Una opción del desplegable: lo que se guarda y lo que se lee. */
export interface OpcionDesplegable {
  valor: string | number;
  etiqueta: string;
  /** Marcador de posición del tipo «Selecciona un alumno», que no es elegible. */
  deshabilitada?: boolean;
}

/**
 * Desplegable de una lista corta: el `<select>` nativo del tema, con el mismo
 * aspecto que los de la ficha de alumno. Estaba escrito a mano en nueve
 * pantallas, siempre igual —la clase, el marcador y el `@for` de opciones—, y
 * con dos formas distintas de leerlo según si la pantalla usa formulario o no.
 *
 * Sigue siendo un `<select>` de verdad: la lista la pinta el navegador, que
 * para cuatro opciones es lo correcto (teclado, móvil y accesibilidad gratis).
 * Cuando hay que buscar entre muchas, lo que toca es `app-selector-busqueda`.
 *
 * Se usa de tres maneras, según lo que ya hacía cada pantalla:
 * - con `[control]`, dentro de un formulario reactivo;
 * - con `(cambia)`, cuando el valor se lleva a la URL;
 * - con una referencia de plantilla, leyendo `elegido()` al guardar.
 */
@Component({
  selector: 'app-desplegable',
  imports: [NgTemplateOutlet, ReactiveFormsModule],
  templateUrl: './desplegable.html',
  host: { class: 'u-contenidos' },
})
export class DesplegableComponent {
  readonly opciones = input<readonly OpcionDesplegable[]>([]);
  /** Control del formulario reactivo. Con él, del valor manda Angular. */
  readonly control = input<FormControl | null>(null);
  /** Valor inicial cuando no hay formulario detrás. */
  readonly valor = input<string | number | null>(null);
  readonly id = input<string>();
  /** Rótulo para el lector de pantalla cuando no hay un `<label>` al lado. */
  readonly etiqueta = input<string>();
  readonly cambia = output<string>();

  /** Lo elegido ahora mismo; lo leen las plantillas que guardan al pulsar un botón. */
  readonly elegido = linkedSignal(() => `${this.valor() ?? ''}`);

  protected esElegida(opcion: OpcionDesplegable): boolean {
    // Con formulario el `selected` lo pone Angular, y pisarlo aquí se pelearía.
    return !this.control() && `${opcion.valor}` === this.elegido();
  }

  protected elegir(valor: string): void {
    this.elegido.set(valor);
    this.cambia.emit(valor);
  }
}
