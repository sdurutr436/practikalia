import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { EstadoComponent } from '../../compartido/estado/estado';
import { EmpresaService } from '../empresa.service';
import { Etiqueta, PaginaEmpresas, esVistaProfesor } from '../empresa.model';
import { PerfilService } from '../../perfil/perfil.service';
import { TarjetaEmpresaComponent } from '../tarjeta-empresa/tarjeta-empresa';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { IconoComponent } from '../../compartido/icono/icono';
import { BotonComponent } from '../../compartido/boton/boton';
import { BuscadorComponent } from '../../compartido/buscador/buscador';
import { DesplegableComponent } from '../../compartido/desplegable/desplegable';
import { PaginacionComponent } from '../../compartido/paginacion/paginacion';
import { PastillasComponent } from '../../compartido/pastillas/pastillas';

/** Las pastillas de publicación. La clave vacía es «Todas», que va sin parámetro. */
const PASTILLAS = [
  { clave: '', etiqueta: 'Todas' },
  { clave: 'true', etiqueta: 'Publicadas' },
  { clave: 'false', etiqueta: 'Sin publicar' },
] as const;

/** Tres columnas por tres filas: lo que cabe sin desplazarse. */
const POR_PAGINA = 9;

@Component({
  selector: 'app-empresas-listado-page',
  imports: [
    EstadoComponent,
    TarjetaEmpresaComponent,
    CabeceraComponent,
    IconoComponent,
    BotonComponent,
    BuscadorComponent,
    DesplegableComponent,
    PaginacionComponent,
    PastillasComponent,
  ],
  templateUrl: './empresas-listado-page.html',
})
export class EmpresasListadoPage {
  private readonly empresaService = inject(EmpresaService);
  private readonly perfilService = inject(PerfilService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly esVistaProfesor = esVistaProfesor;
  protected readonly pastillas = PASTILLAS;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly resultado = signal<PaginaEmpresas | null>(null);
  protected readonly catalogo = signal<Etiqueta[]>([]);
  protected readonly filtrosAbiertos = signal(false);

  /**
   * Todo el estado del listado (pastilla, búsqueda, filtros y página) vive en
   * la URL: se comparte por enlace, sobrevive a recargar y el botón de atrás
   * hace lo que se espera. Leerlo reactivo es obligatorio — cambiarlo no
   * recrea el componente.
   */
  private readonly parametros = toSignal(this.route.queryParamMap, { requireSync: true });
  protected readonly filtro = computed(() => this.parametros().get('publicada'));
  /** La pastilla activa; sin parámetro, la de clave vacía. */
  protected readonly filtroActivo = computed(() => this.filtro() ?? '');
  protected readonly texto = computed(() => this.parametros().get('texto') ?? '');
  protected readonly sectorId = computed(() => this.parametros().get('sectorId') ?? '');
  protected readonly etiquetaIds = computed(() =>
    (this.parametros().get('etiquetaIds') ?? '').split(',').filter(Boolean).map(Number),
  );
  protected readonly paginaActual = computed(() => Number(this.parametros().get('pagina') ?? 0));

  protected readonly esProfesor = computed(() => this.auth.sesion()?.rol !== 'ALUMNO');
  protected readonly empresas = computed(() => this.resultado()?.contenido ?? []);
  protected readonly paginas = computed(() => this.resultado()?.paginas ?? 0);
  protected readonly filtrosActivos = computed(
    () => (this.sectorId() ? 1 : 0) + this.etiquetaIds().length,
  );

  protected readonly mensajeVacio = computed(() => {
    if (this.texto() || this.filtrosActivos() > 0) {
      return 'Ninguna empresa coincide con la búsqueda.';
    }
    return this.filtro() === null ? 'Todavía no hay empresas.' : 'Ninguna empresa con ese filtro.';
  });

  constructor() {
    effect(() => {
      this.parametros();
      untracked(() => void this.cargar());
    });
  }

  protected buscar(texto: string): void {
    this.navegar({ texto: texto || null });
  }

  /** El catálogo de etiquetas solo hace falta si alguien abre los filtros. */
  protected async alternarFiltros(): Promise<void> {
    this.filtrosAbiertos.update((abierto) => !abierto);
    if (this.filtrosAbiertos() && this.catalogo().length === 0) {
      this.catalogo.set(await this.perfilService.listarEtiquetas());
    }
  }

  /** El catálogo de sectores tal y como lo pide el desplegable. */
  protected readonly opcionesSector = computed(() => [
    { valor: '', etiqueta: 'Cualquiera' },
    ...this.catalogo().map((etiqueta) => ({ valor: etiqueta.id, etiqueta: etiqueta.nombre })),
  ]);

  protected cambiarSector(valor: string): void {
    this.navegar({ sectorId: valor || null });
  }

  protected alternarEtiqueta(id: number): void {
    const ids = this.etiquetaIds().includes(id)
      ? this.etiquetaIds().filter((etiquetaId) => etiquetaId !== id)
      : [...this.etiquetaIds(), id];
    this.navegar({ etiquetaIds: ids.join(',') || null });
  }

  protected limpiarFiltros(): void {
    this.navegar({ sectorId: null, etiquetaIds: null });
  }

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pagina: pagina || null },
      queryParamsHandling: 'merge',
    });
  }

  /** Tocar un filtro devuelve siempre a la primera página. */
  private navegar(cambios: Record<string, string | null>): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pagina: null, ...cambios },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    try {
      this.resultado.set(
        await this.empresaService.listar({
          texto: this.texto() || null,
          publicada: this.filtro() === null ? null : this.filtro() === 'true',
          sectorId: this.sectorId() ? Number(this.sectorId()) : null,
          etiquetaIds: this.etiquetaIds(),
          pagina: this.paginaActual(),
          tamano: POR_PAGINA,
        }),
      );
      this.error.set(null);
    } catch {
      this.error.set('No se pudieron cargar las empresas.');
    } finally {
      this.cargando.set(false);
    }
  }
}
