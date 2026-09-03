import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MENSAJES_CATALOGO, mensajeDeError } from '../../auth/mensajes-error';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import {
  AltaCatalogo,
  ColumnaCatalogoComponent,
  ItemCatalogo,
} from '../../compartido/columna-catalogo/columna-catalogo';
import { EstadoComponent } from '../../compartido/estado/estado';
import { ToastService } from '../../compartido/toast/toast.service';
import { CatalogoService, Nodo } from '../catalogo.service';

const numero = (valor: string | null) => (valor ? Number(valor) : null);

/** Un nodo con el contador de lo que le cuelga; la columna lo pinta a la derecha. */
const conCuenta = (nodo: Nodo, grupo?: string): ItemCatalogo => ({
  id: nodo.id,
  nombre: nodo.nombre,
  cuenta: nodo.hijas.length,
  grupo,
});

/**
 * Mantenimiento del catálogo, de izquierda a derecha: se elige o se crea un
 * sector, luego su actividad principal y luego las etiquetas de esa actividad.
 * Las tres columnas son el mismo componente; lo único que cambia es de quién
 * cuelga lo que se crea en cada una.
 *
 * La columna de la izquierda lleva además los grupos transversales, que no son
 * sectores: sus etiquetas —las modalidades de trabajo— valen para cualquier
 * empresa, así que ni se eligen como sector ni cuelgan de uno. Elegido un
 * grupo transversal, la segunda columna ya son sus etiquetas y no hay tercera.
 *
 * Lo elegido viaja en la URL (`?sector=&actividad=`), igual que en el resto de
 * pantallas: recargar no pierde el sitio y el enlace se puede compartir.
 */
@Component({
  selector: 'app-sectores-page',
  imports: [CabeceraComponent, EstadoComponent, AlertaComponent, ColumnaCatalogoComponent],
  templateUrl: './sectores-page.html',
})
export class SectoresPage {
  private readonly catalogo = inject(CatalogoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly fallo = signal<string | null>(null);
  protected readonly ocupada = signal(false);
  private readonly arbol = signal<Nodo[]>([]);

  private readonly parametros = toSignal(this.route.queryParamMap, { requireSync: true });
  protected readonly sectorId = computed(() => numero(this.parametros().get('sector')));
  protected readonly actividadId = computed(() => numero(this.parametros().get('actividad')));

  private readonly sector = computed(() =>
    this.arbol().find((nodo) => nodo.id === this.sectorId()),
  );
  private readonly actividad = computed(() =>
    this.sector()?.hijas.find((nodo) => nodo.id === this.actividadId()),
  );

  /** Elegido un grupo transversal, lo que cuelga ya son etiquetas y no actividades. */
  protected readonly esTransversal = computed(() => this.sector()?.transversal ?? false);

  /**
   * Los sectores primero y los grupos transversales debajo. Solo estos llevan
   * rótulo de grupo: marca dónde dejan de ser sectores, y el de arriba ya lo
   * dice el título de la columna.
   */
  protected readonly sectores = computed<ItemCatalogo[]>(() =>
    this.arbol().map((nodo) => conCuenta(nodo, nodo.transversal ? 'Transversales' : undefined)),
  );

  protected readonly actividades = computed<ItemCatalogo[]>(() =>
    (this.sector()?.hijas ?? []).map((nodo) =>
      this.esTransversal() ? { id: nodo.id, nombre: nodo.nombre } : conCuenta(nodo),
    ),
  );

  protected readonly etiquetas = computed<ItemCatalogo[]>(() =>
    (this.actividad()?.hijas ?? []).map((nodo) => ({ id: nodo.id, nombre: nodo.nombre })),
  );

  protected readonly tituloActividades = computed(() =>
    this.esTransversal() ? 'Etiquetas transversales' : 'Actividad principal',
  );

  protected readonly vacioActividades = computed(() =>
    this.esTransversal()
      ? 'Este grupo todavía no tiene etiquetas.'
      : 'Este sector todavía no tiene actividades. Sin ellas, una empresa del sector solo elige el sector.',
  );

  /** Columna activa solo cuando hay de quién colgar lo que se cree en ella. */
  protected readonly hayActividades = computed(() => this.sector() !== undefined);
  protected readonly hayEtiquetas = computed(
    () => !this.esTransversal() && this.actividad() !== undefined,
  );

  constructor() {
    // El árbol se pide una vez: lo elegido es estado de pantalla, no un filtro
    // del servidor, así que cambiar de sector no vuelve a preguntar.
    void this.cargar();
  }

  protected elegirSector(id: number): void {
    this.navegar({ sector: id, actividad: null });
  }

  protected elegirActividad(id: number): void {
    this.navegar({ actividad: id });
  }

  protected crearSector({ nombre, marcada }: AltaCatalogo): Promise<boolean> {
    return this.ejecutar(
      () => this.catalogo.crear(nombre, null, marcada),
      marcada ? `Grupo transversal «${nombre}» creado.` : `Sector «${nombre}» creado.`,
    );
  }

  protected crearActividad({ nombre }: AltaCatalogo): Promise<boolean> {
    const padre = this.sectorId();
    return padre === null
      ? Promise.resolve(false)
      : this.ejecutar(() => this.catalogo.crear(nombre, padre), `«${nombre}» creada.`);
  }

  protected crearEtiqueta({ nombre }: AltaCatalogo): Promise<boolean> {
    const padre = this.actividadId();
    return padre === null
      ? Promise.resolve(false)
      : this.ejecutar(() => this.catalogo.crear(nombre, padre), `Etiqueta «${nombre}» creada.`);
  }

  protected renombrar({ id, nombre }: { id: number; nombre: string }): Promise<boolean> {
    return this.ejecutar(() => this.catalogo.renombrar(id, nombre), `Ahora se llama «${nombre}».`);
  }

  protected async borrar(id: number): Promise<void> {
    if (!(await this.ejecutar(() => this.catalogo.borrar(id), 'Borrado.'))) {
      return;
    }
    // Borrado lo que estaba elegido, la URL se queda apuntando a un id que ya
    // no existe y las columnas de la derecha no sabrían qué pintar.
    if (id === this.sectorId()) {
      this.navegar({ sector: null, actividad: null });
    } else if (id === this.actividadId()) {
      this.navegar({ actividad: null });
    }
  }

  private navegar(queryParams: Record<string, unknown>): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge',
    });
  }

  /**
   * Toda acción del catálogo: guarda, recarga el árbol y avisa, o pinta el
   * motivo. Devuelve si salió, que es lo que mira el borrado antes de tocar
   * lo que hay elegido.
   */
  private async ejecutar(accion: () => Promise<unknown>, hecho: string): Promise<boolean> {
    if (this.ocupada()) {
      return false;
    }
    this.ocupada.set(true);
    this.fallo.set(null);
    try {
      await accion();
      await this.cargar();
      this.toast.mostrar(hecho);
      return true;
    } catch (e) {
      this.fallo.set(mensajeDeError(e, MENSAJES_CATALOGO));
      return false;
    } finally {
      this.ocupada.set(false);
    }
  }

  private async cargar(): Promise<void> {
    try {
      this.arbol.set(await this.catalogo.arbol());
      this.error.set(null);
    } catch {
      this.error.set('No se pudo cargar el catálogo de sectores.');
    } finally {
      this.cargando.set(false);
    }
  }
}
