import {
  Component,
  ElementRef,
  computed,
  effect,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { IconoComponent } from '../icono/icono';

/** Una opción del desplegable: lo que se guarda y lo que se lee. */
export interface OpcionDesplegable {
  valor: string | number;
  etiqueta: string;
}

/** Sin acentos ni mayúsculas: buscar «bahia» tiene que encontrar «Bahía Solar». */
const normalizar = (texto: string) =>
  texto
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();

/** Ids únicos por instancia, que puede haber uno por fila de un listado. */
let contador = 0;

/**
 * El desplegable de la aplicación: una entrada con su lista, escrita por
 * nosotros. El `<select>` nativo pinta su lista con el cromo del navegador
 * —fondo blanco, resaltado azul del sistema, esquinas rectas—, que es lo único
 * de la pantalla que no sigue el tema; esta cuelga como el menú de hamburguesa
 * y se puede filtrar tecleando, que con veinte empresas hace falta.
 *
 * Se usa de tres maneras, según lo que ya hacía cada pantalla:
 * - con `[control]`, dentro de un formulario reactivo;
 * - con `[valor]` y `(cambia)`, cuando lo elegido se lleva a la URL;
 * - con una referencia de plantilla, leyendo `elegido()` al guardar.
 *
 * ponytail: no implementa `ControlValueAccessor`. Con `[control]` se escribe y
 * se lee el mismo `FormControl` a mano, que es todo lo que hacía falta y
 * respeta la decisión de la F1 de no meter CVA en el proyecto.
 */
@Component({
  selector: 'app-desplegable',
  imports: [IconoComponent],
  templateUrl: './desplegable.html',
  // El `id` es una entrada que se pinta en el <input> de dentro: sin esto, el
  // atributo estático se queda también en el anfitrión y hay dos elementos con
  // el mismo id (y el `for` del rótulo apunta al que no es un control).
  host: { class: 'u-contenidos', '[attr.id]': 'null' },
})
export class DesplegableComponent {
  readonly opciones = input<readonly OpcionDesplegable[]>([]);
  /** Control del formulario reactivo, si la pantalla tiene uno detrás. */
  readonly control = input<FormControl | null>(null);
  /** Valor de partida cuando no hay formulario. */
  readonly valor = input<string | number | null>(null);
  readonly id = input<string>();
  /** Rótulo para el lector de pantalla cuando no hay un `<label>` al lado. */
  readonly etiqueta = input<string>();
  /** Lo que se lee con la lista cerrada y nada elegido. */
  readonly marcador = input('Sin elegir');
  readonly vacio = input('Sin resultados.');
  readonly cambia = output<string>();

  /** Lo elegido ahora mismo, como texto. Lo leen las plantillas al guardar. */
  readonly elegido = signal('');

  protected readonly abierto = signal(false);
  protected readonly resaltada = signal(0);
  private readonly busqueda = signal('');

  protected readonly lista = `desplegable-lista-${++contador}`;

  private readonly entrada = viewChild.required<ElementRef<HTMLInputElement>>('entrada');
  private readonly raiz = viewChild.required<ElementRef<HTMLElement>>('raiz');

  /** La lista no cabe debajo y se despliega hacia arriba. */
  protected readonly haciaArriba = signal(false);

  constructor() {
    // El valor puede venir de fuera de dos sitios, y los dos hay que seguirlos:
    // el `[valor]` de entrada y los cambios del control del formulario.
    effect((alLimpiar) => {
      const control = this.control();
      if (!control) {
        this.elegido.set(`${this.valor() ?? ''}`);
        return;
      }
      this.elegido.set(`${control.value ?? ''}`);
      const suscripcion = control.valueChanges.subscribe((nuevo) =>
        this.elegido.set(`${nuevo ?? ''}`),
      );
      alLimpiar(() => suscripcion.unsubscribe());
    });
  }

  private readonly opcionElegida = computed(() =>
    this.opciones().find((opcion) => `${opcion.valor}` === this.elegido()),
  );

  /** Abierto manda lo tecleado; cerrado, el nombre de lo que hay elegido. */
  protected readonly texto = computed(() =>
    this.abierto() ? this.busqueda() : (this.opcionElegida()?.etiqueta ?? ''),
  );

  /** Abierto la entrada está vacía, así que el marcador recuerda lo ya elegido. */
  protected readonly marcadorActual = computed(
    () => this.opcionElegida()?.etiqueta ?? this.marcador(),
  );

  protected readonly filtradas = computed(() => {
    const buscado = normalizar(this.busqueda().trim());
    return buscado
      ? this.opciones().filter((opcion) => normalizar(opcion.etiqueta).includes(buscado))
      : this.opciones();
  });

  /**
   * El ancho del `<input>` en caracteres: el de su opción más larga y uno de
   * propina, que `size` cuenta anchos medios y las mayúsculas se salen.
   */
  protected readonly tamano = computed(
    () =>
      1 +
      Math.max(
        this.marcadorActual().length,
        ...this.opciones().map((opcion) => opcion.etiqueta.length),
      ),
  );

  protected opcionId(indice: number): string {
    return `${this.lista}-${indice}`;
  }

  protected esElegida(opcion: OpcionDesplegable): boolean {
    return `${opcion.valor}` === this.elegido();
  }

  /** Se abre con la búsqueda vacía: se ve la lista entera, no filtrada por lo ya elegido. */
  protected abrir(): void {
    this.busqueda.set('');
    this.resaltada.set(Math.max(this.opciones().indexOf(this.opcionElegida()!), 0));
    this.ubicar();
    this.abierto.set(true);
    this.desplazar();
  }

  /**
   * Se abre hacia el lado que tenga más sitio. Así no hay que saber aquí
   * cuánto mide la lista —eso lo dice su hoja de estilos— ni comparar contra
   * ninguna medida escrita a mano, y el último campo de un modal deja de
   * desplegarse contra el borde.
   */
  private ubicar(): void {
    const caja = this.raiz().nativeElement.getBoundingClientRect();
    this.haciaArriba.set(window.innerHeight - caja.bottom < caja.top);
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

  protected elegir(opcion: OpcionDesplegable): void {
    this.cerrar();
    this.elegido.set(`${opcion.valor}`);
    const control = this.control();
    if (control) {
      control.setValue(opcion.valor);
      control.markAsTouched();
      // `setValue` no ensucia el control por sí solo, y esto es alguien
      // eligiendo a mano: sin esto, un formulario que solo cambia aquí se
      // cierra sin avisar de que hay algo sin guardar.
      control.markAsDirty();
    }
    this.cambia.emit(`${opcion.valor}`);
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
          this.entrada().nativeElement.blur();
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
