import { Component, computed, input, output, signal } from '@angular/core';
import { IconoComponent } from '../icono/icono';

/** Lo mínimo que necesita el selector de cada opción. */
export interface OpcionSelector {
  id: number;
  nombre: string;
}

/** Sin acentos ni mayúsculas: buscar «bahia» tiene que encontrar «Bahía Solar». */
const normalizar = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();

/** Ids únicos por instancia, que hay uno de estos por fila del listado. */
let contador = 0;

/**
 * Desplegable con búsqueda. Existe porque el `<datalist>` nativo —que hace
 * justo esto sin escribir código— pinta su lista con el cromo del navegador:
 * no admite CSS ni hereda el tema, así que en medio de la aplicación se veía
 * como una pieza traída de otro sitio. La lista de aquí es de la aplicación y
 * sale de los mismos tokens que el resto.
 */
@Component({
  selector: 'app-selector-busqueda',
  imports: [IconoComponent],
  templateUrl: './selector-busqueda.html',
  host: { class: 'u-crecer' },
})
export class SelectorBusquedaComponent {
  readonly opciones = input.required<OpcionSelector[]>();
  /** Id de la opción elegida, o `null`. */
  readonly valor = input<number | null>(null);
  /** Rótulo para el lector de pantalla: la entrada no lleva `<label>` visible. */
  readonly etiqueta = input.required<string>();
  readonly marcador = input('Sin asignar');
  readonly vacio = input('Sin resultados.');
  readonly elegida = output<number>();

  protected readonly abierto = signal(false);
  protected readonly resaltada = signal(0);
  private readonly busqueda = signal('');

  protected readonly lista = `selector-lista-${++contador}`;

  private readonly opcionElegida = computed(
    () => this.opciones().find((opcion) => opcion.id === this.valor()) ?? null,
  );

  /** Abierto manda lo tecleado; cerrado, el nombre de lo que hay elegido. */
  protected readonly texto = computed(() =>
    this.abierto() ? this.busqueda() : (this.opcionElegida()?.nombre ?? ''),
  );

  /** Abierto la entrada está vacía, así que el marcador recuerda lo ya elegido. */
  protected readonly marcadorActual = computed(
    () => this.opcionElegida()?.nombre ?? this.marcador(),
  );

  protected readonly filtradas = computed(() => {
    const buscado = normalizar(this.busqueda().trim());
    return buscado
      ? this.opciones().filter((opcion) => normalizar(opcion.nombre).includes(buscado))
      : this.opciones();
  });

  protected opcionId(indice: number): string {
    return `${this.lista}-${indice}`;
  }

  /** Se abre con la búsqueda vacía: se ve el catálogo entero, no filtrado por lo ya elegido. */
  protected abrir(): void {
    this.busqueda.set('');
    this.resaltada.set(0);
    this.abierto.set(true);
  }

  protected escribir(valor: string): void {
    this.busqueda.set(valor);
    this.resaltada.set(0);
    this.abierto.set(true);
  }

  protected cerrar(): void {
    this.abierto.set(false);
    this.busqueda.set('');
  }

  protected elegir(opcion: OpcionSelector): void {
    this.cerrar();
    this.elegida.emit(opcion.id);
  }

  protected teclear(evento: KeyboardEvent): void {
    const opciones = this.filtradas();
    switch (evento.key) {
      case 'ArrowDown':
      case 'ArrowUp':
        evento.preventDefault();
        if (!this.abierto()) {
          this.abrir();
          return;
        }
        if (opciones.length > 0) {
          // Da la vuelta por los dos extremos: con listas cortas es lo natural.
          const paso = evento.key === 'ArrowDown' ? 1 : -1;
          this.resaltada.update((i) => (i + paso + opciones.length) % opciones.length);
          this.desplazar();
        }
        break;
      case 'Enter':
        if (this.abierto() && opciones[this.resaltada()]) {
          evento.preventDefault();
          this.elegir(opciones[this.resaltada()]);
        }
        break;
      case 'Escape':
        this.cerrar();
        break;
      default:
    }
  }

  /** Sin esto, flechar más allá de lo visible deja el resaltado fuera de la vista. */
  private desplazar(): void {
    queueMicrotask(() =>
      document
        .getElementById(this.opcionId(this.resaltada()))
        ?.scrollIntoView({ block: 'nearest' }),
    );
  }
}
