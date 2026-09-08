import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MENSAJES_ASIGNACION, mensajeDeError } from '../../auth/mensajes-error';
import { AsignacionService } from '../asignacion.service';
import { Asignacion, PaginaAsignaciones, textoContratacion } from '../asignacion.model';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { VolverComponent } from '../../compartido/volver/volver';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { EstadoComponent } from '../../compartido/estado/estado';
import { CampoComponent } from '../../compartido/campo/campo';
import { BotonComponent } from '../../compartido/boton/boton';
import { DesplegableComponent } from '../../compartido/desplegable/desplegable';
import { PaginacionComponent } from '../../compartido/paginacion/paginacion';

/** Las tres respuestas posibles a «¿acabó contratado?»; `''` es «todavía no se sabe». */
const CONTRATACION = [
  { valor: '', etiqueta: 'Sin decidir' },
  { valor: 'true', etiqueta: 'Contratado' },
  { valor: 'false', etiqueta: 'No contratado' },
];

const POR_PAGINA = 10;

/**
 * Histórico paginado de asignaciones de una empresa, solo para profesorado
 * (`profesorGuard`). Antes vivía embebido y sin paginar en la propia ficha de
 * la empresa; con muchas prácticas acumuladas, esa lista crecía sin límite.
 */
@Component({
  selector: 'app-empresa-asignaciones-page',
  imports: [
    RouterLink,
    CabeceraComponent,
    VolverComponent,
    AlertaComponent,
    EstadoComponent,
    CampoComponent,
    BotonComponent,
    DesplegableComponent,
    PaginacionComponent,
  ],
  templateUrl: './empresa-asignaciones-page.html',
})
export class EmpresaAsignacionesPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly asignacionService = inject(AsignacionService);

  protected readonly empresaId = Number(this.route.snapshot.paramMap.get('empresaId'));
  protected readonly textoContratacion = textoContratacion;
  protected readonly CONTRATACION = CONTRATACION;

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly resultado = signal<PaginaAsignaciones | null>(null);

  protected readonly guardandoId = signal<number | null>(null);
  protected readonly errorCierre = signal<{ id: number; mensaje: string } | null>(null);

  private readonly parametros = toSignal(this.route.queryParamMap, { requireSync: true });
  protected readonly paginaActual = computed(() => Number(this.parametros().get('pagina') ?? 0));

  protected readonly asignaciones = computed(() => this.resultado()?.contenido ?? []);
  protected readonly paginas = computed(() => this.resultado()?.paginas ?? 0);
  protected readonly total = computed(() => this.resultado()?.total ?? 0);

  constructor() {
    effect(() => {
      this.paginaActual();
      untracked(() => void this.cargar());
    });
  }

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pagina: pagina || null },
      queryParamsHandling: 'merge',
    });
  }

  /** Con qué opción arranca el desplegable de contratación de esa asignación. */
  protected textoContratado(asignacion: Asignacion): string {
    return asignacion.contratadoPosterior === null ? '' : `${asignacion.contratadoPosterior}`;
  }

  protected async cerrarAsignacion(
    asignacion: Asignacion,
    fechaFin: string,
    contratadoTexto: string,
  ): Promise<void> {
    if (!fechaFin || this.guardandoId() !== null) {
      return;
    }
    this.guardandoId.set(asignacion.id);
    this.errorCierre.set(null);
    const contratadoPosterior = contratadoTexto === '' ? null : contratadoTexto === 'true';
    try {
      const actualizada = await this.asignacionService.cerrar(asignacion.id, {
        fechaFin,
        contratadoPosterior,
      });
      this.resultado.update((resultado) =>
        resultado === null
          ? resultado
          : {
              ...resultado,
              contenido: resultado.contenido.map((a) => (a.id === actualizada.id ? actualizada : a)),
            },
      );
    } catch (e) {
      this.errorCierre.set({ id: asignacion.id, mensaje: mensajeDeError(e, MENSAJES_ASIGNACION) });
    } finally {
      this.guardandoId.set(null);
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    try {
      this.resultado.set(
        await this.asignacionService.listarPorEmpresa(this.empresaId, this.paginaActual(), POR_PAGINA),
      );
      this.error.set(null);
    } catch {
      this.error.set('No se pudieron cargar las asignaciones.');
    } finally {
      this.cargando.set(false);
    }
  }
}
