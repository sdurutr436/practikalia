import { Component, effect, input, output, signal, untracked } from '@angular/core';
import { IconoComponent } from '../icono/icono';

/** Una fila de la columna. */
export interface ItemCatalogo {
  id: number;
  nombre: string;
  /** Lo que cuelga de él; se pinta a la derecha. Sin valor, no se pinta nada. */
  cuenta?: number;
  /** Rótulo de separación: al cambiar respecto al anterior, se pinta encima. */
  grupo?: string;
}

/** Lo que emite el alta: el nombre y, si la columna la ofrece, su casilla. */
export interface AltaCatalogo {
  nombre: string;
  marcada: boolean;
}

/**
 * Columna de mantenimiento de un catálogo: lista lo que hay en un nivel y deja
 * elegir, renombrar, borrar y añadir sin salir de ella.
 *
 * Existe porque la pantalla de sectores son tres columnas idénticas —sectores,
 * actividades y etiquetas— encadenadas de izquierda a derecha: lo único que
 * cambia entre ellas son los textos y de quién cuelga lo que se crea, y eso lo
 * decide la pantalla, no la columna.
 *
 * No guarda nada por su cuenta: cada acción sale como evento y es el padre
 * quien llama al servidor y vuelve a alimentar `items`.
 */
@Component({
  selector: 'app-columna-catalogo',
  imports: [IconoComponent],
  templateUrl: './columna-catalogo.html',
  host: { class: 'u-contenidos' },
})
export class ColumnaCatalogoComponent {
  readonly titulo = input.required<string>();
  readonly items = input<readonly ItemCatalogo[]>([]);
  readonly elegido = input<number | null>(null);
  /** Apagada mientras no haya nada elegido en la columna de la izquierda. */
  readonly activa = input(true);
  readonly pista = input('Elige algo en la columna anterior.');
  readonly vacio = input('Todavía no hay nada aquí.');
  readonly marcador = input('Nombre…');
  /** Si se pasa, el alta ofrece una casilla con este rótulo. */
  readonly marcaAlta = input<string>();
  /** Hay una petición en curso: se bloquean los botones de toda la columna. */
  readonly ocupada = input(false);

  readonly elegir = output<number>();
  readonly crear = output<AltaCatalogo>();
  readonly renombrar = output<{ id: number; nombre: string }>();
  readonly borrar = output<number>();

  /** Fila que se está renombrando ahora mismo, y el nombre a medio escribir. */
  protected readonly editando = signal<number | null>(null);
  protected readonly borrador = signal('');
  /** Fila con el borrado pedido, a la espera de confirmar. */
  protected readonly confirmando = signal<number | null>(null);
  protected readonly nuevo = signal('');
  protected readonly marcado = signal(false);

  constructor() {
    // El alta se vacía cuando la lista crece, no al pulsar: si el guardado
    // falla —lo normal es que el nombre ya exista— lo tecleado sigue ahí para
    // corregirlo en vez de tener que escribirlo otra vez.
    let anteriores = this.items().length;
    effect(() => {
      const cuantos = this.items().length;
      untracked(() => {
        if (cuantos > anteriores) {
          this.nuevo.set('');
          this.marcado.set(false);
        }
        anteriores = cuantos;
      });
    });
  }

  protected abreGrupo(indice: number): boolean {
    const items = this.items();
    return !!items[indice].grupo && items[indice].grupo !== items[indice - 1]?.grupo;
  }

  protected empezarRenombrado(item: ItemCatalogo): void {
    this.confirmando.set(null);
    this.borrador.set(item.nombre);
    this.editando.set(item.id);
  }

  protected confirmarRenombrado(item: ItemCatalogo): void {
    const nombre = this.borrador().trim();
    this.editando.set(null);
    if (nombre && nombre !== item.nombre) {
      this.renombrar.emit({ id: item.id, nombre });
    }
  }

  protected pedirBorrado(item: ItemCatalogo): void {
    this.editando.set(null);
    this.confirmando.set(item.id);
  }

  protected confirmarBorrado(item: ItemCatalogo): void {
    this.confirmando.set(null);
    this.borrar.emit(item.id);
  }

  protected anadir(): void {
    const nombre = this.nuevo().trim();
    if (nombre) {
      this.crear.emit({ nombre, marcada: this.marcado() });
    }
  }
}
