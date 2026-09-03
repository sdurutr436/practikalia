import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Alumno, PaginaAlumnos, nombreCompleto } from '../../alumnado/alumnado.service';
import { SelectorCursoComponent } from '../../alumnado/selector-curso/selector-curso';
import { MENSAJES_ASIGNACION, mensajeDeError } from '../../auth/mensajes-error';
import { GradoOpcion, RegistroService } from '../../auth/registro.service';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';
import { BuscadorComponent } from '../../compartido/buscador/buscador';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { DesplegableComponent } from '../../compartido/desplegable/desplegable';
import { EstadoComponent } from '../../compartido/estado/estado';
import { PaginacionComponent } from '../../compartido/paginacion/paginacion';
import { PastillasComponent } from '../../compartido/pastillas/pastillas';
import { ToastService } from '../../compartido/toast/toast.service';
import { Empresa } from '../../empresas/empresa.model';
import { EmpresaService } from '../../empresas/empresa.service';
import { AsignacionService } from '../asignacion.service';

/** Filas apiladas y no tarjetas: caben más por pantalla que las 3×3 de los listados. */
const POR_PAGINA = 10;

/** Las pastillas. La clave viaja en `?estado=`, igual que en alumnado y reseñas. */
const PASTILLAS = [
  { clave: 'todas', asignado: null, etiqueta: 'Todas' },
  { clave: 'asignados', asignado: true, etiqueta: 'Asignados' },
  { clave: 'sin-asignar', asignado: false, etiqueta: 'Sin asignar' },
] as const;

const VACIOS: Record<string, string> = {
  todas: 'No hay alumnado matriculado en este curso.',
  asignados: 'Todavía no hay ningún alumno con empresa.',
  'sin-asignar': 'Todo el alumnado del curso tiene ya su empresa.',
};

const numero = (valor: string | null) => (valor ? Number(valor) : null);

/**
 * Asignaciones de un curso: una fila por alumno con el desplegable de empresa
 * y su propio botón de guardar. Solo sale el alumnado del curso elegido —se
 * asume que las prácticas se hacen en el curso de matrícula y que ese curso no
 * se repite—, y solo se ofrecen empresas confirmadas (`publicada`).
 *
 * Todo el estado (pastilla, búsqueda, clase, curso y página) vive en la URL,
 * igual que en el listado de empresas: se comparte por enlace y sobrevive a
 * recargar.
 */
@Component({
  selector: 'app-asignaciones-page',
  imports: [
    CabeceraComponent,
    EstadoComponent,
    AlertaComponent,
    BotonComponent,
    BuscadorComponent,
    DesplegableComponent,
    PaginacionComponent,
    PastillasComponent,
    SelectorCursoComponent,
  ],
  templateUrl: './asignaciones-page.html',
})
export class AsignacionesPage {
  private readonly asignacionService = inject(AsignacionService);
  private readonly empresaService = inject(EmpresaService);
  private readonly registroService = inject(RegistroService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly pastillas = PASTILLAS;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly resultado = signal<PaginaAlumnos | null>(null);
  protected readonly empresas = signal<Empresa[]>([]);
  protected readonly grados = signal<GradoOpcion[]>([]);
  protected readonly guardandoId = signal<number | null>(null);
  protected readonly errorFila = signal<{ id: number; mensaje: string } | null>(null);

  /** Empresa elegida en cada fila mientras no se guarda, por id de alumno. */
  private readonly seleccion = signal<Record<number, number>>({});

  private readonly parametros = toSignal(this.route.queryParamMap, { requireSync: true });

  protected readonly clave = computed(() => {
    const valor = this.parametros().get('estado');
    return PASTILLAS.some((pastilla) => pastilla.clave === valor) ? valor! : 'todas';
  });
  private readonly asignado = computed(
    () => PASTILLAS.find((pastilla) => pastilla.clave === this.clave())!.asignado,
  );
  protected readonly texto = computed(() => this.parametros().get('texto') ?? '');
  protected readonly gradoId = computed(() => numero(this.parametros().get('gradoId')));
  protected readonly anio = computed(() => numero(this.parametros().get('anio')));
  protected readonly paginaActual = computed(() => Number(this.parametros().get('pagina') ?? 0));

  protected readonly alumnos = computed(() => this.resultado()?.contenido ?? []);
  protected readonly paginas = computed(() => this.resultado()?.paginas ?? 0);
  protected readonly total = computed(() => this.resultado()?.total ?? 0);
  /** Los catálogos con la forma que pide el desplegable. */
  protected readonly opcionesClase = computed(() => [
    { valor: '', etiqueta: 'Todas las clases' },
    ...this.grados().map((grado) => ({ valor: grado.id, etiqueta: grado.nombre })),
  ]);
  protected readonly opcionesEmpresa = computed(() =>
    this.empresas().map((empresa) => ({ valor: empresa.id, etiqueta: empresa.nombre })),
  );

  protected readonly mensajeVacio = computed(() =>
    this.texto() || this.gradoId() !== null
      ? 'Ningún alumno coincide con la búsqueda.'
      : VACIOS[this.clave()],
  );

  constructor() {
    this.cargarCatalogos();
    effect(() => {
      this.parametros();
      untracked(() => void this.cargar());
    });
  }

  /** Aquí el respaldo es el correo: sin nombre, es lo único que identifica al alumno. */
  protected nombre(alumno: Alumno): string {
    return nombreCompleto(alumno, alumno.correo);
  }

  /** Lo elegido en la fila, o la empresa que ya tiene asignada. */
  protected elegida(alumno: Alumno): number | null {
    return this.seleccion()[alumno.id] ?? alumno.empresaId;
  }

  protected elegir(alumno: Alumno, empresaId: number): void {
    this.seleccion.update((actuales) => ({ ...actuales, [alumno.id]: empresaId }));
    this.errorFila.set(null);
  }

  /** Solo se guarda si la elección cambia lo que ya había. */
  protected puedeGuardar(alumno: Alumno): boolean {
    const elegida = this.elegida(alumno);
    return elegida !== null && elegida !== alumno.empresaId && this.guardandoId() !== alumno.id;
  }

  protected buscar(texto: string): void {
    this.navegar({ texto: texto || null });
  }

  protected filtrar(clave: 'gradoId' | 'anio', valor: string): void {
    this.navegar({ [clave]: valor || null });
  }

  protected irAPagina(pagina: number): void {
    this.navegar({ pagina: pagina || null }, false);
  }

  protected async guardar(alumno: Alumno): Promise<void> {
    const empresaId = this.elegida(alumno);
    if (empresaId === null || this.guardandoId() !== null) {
      return;
    }
    this.guardandoId.set(alumno.id);
    this.errorFila.set(null);
    try {
      const asignacion = await this.asignacionService.asignar(alumno.id, empresaId);
      this.toast.mostrar(`${this.nombre(alumno)} va a ${asignacion.empresaNombre}.`);
      // La elección ya está guardada: lo que se pinte a partir de ahora sale de
      // la respuesta del servidor, no de lo que quedó elegido en la fila.
      this.seleccion.update(({ [alumno.id]: _, ...resto }) => resto);
      await this.cargar();
    } catch (e) {
      this.errorFila.set({ id: alumno.id, mensaje: mensajeDeError(e, MENSAJES_ASIGNACION) });
    } finally {
      this.guardandoId.set(null);
    }
  }

  /** Cambiar de filtro vuelve a la primera página; paginar, obviamente, no. */
  private navegar(queryParams: Record<string, unknown>, alPrincipio = true): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: alPrincipio ? { ...queryParams, pagina: null } : queryParams,
      queryParamsHandling: 'merge',
    });
  }

  /**
   * Los tres catálogos van en paralelo y cada uno por su cuenta: juntos en un
   * `Promise.all`, un fallo de los filtros dejaba la pantalla sin el catálogo de
   * empresas, que es lo único sin lo que no se puede hacer nada aquí.
   */
  private cargarCatalogos(): void {
    void this.empresaService
      .listar({ publicada: true })
      .then((pagina) => this.empresas.set(pagina.contenido))
      .catch(() => this.error.set('No se pudo cargar el catálogo de empresas confirmadas.'));

    // Sin el catálogo de clases la pantalla sigue sirviendo: se pierde filtrar.
    void this.registroService
      .listarGrados()
      .then((grados) => this.grados.set(grados))
      .catch(() => undefined);
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    try {
      this.resultado.set(
        await this.asignacionService.listarAlumnadoDelCurso({
          anio: this.anio(),
          gradoId: this.gradoId(),
          texto: this.texto(),
          asignado: this.asignado(),
          pagina: this.paginaActual(),
          tamano: POR_PAGINA,
        }),
      );
      this.error.set(null);
    } catch {
      this.error.set('No se pudo cargar el alumnado del curso.');
    } finally {
      this.cargando.set(false);
    }
  }
}
