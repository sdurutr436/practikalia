import { Component, computed, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { CarruselComponent } from '../compartido/carrusel/carrusel';
import { EstadoComponent } from '../compartido/estado/estado';
import { EstrellasComponent } from '../compartido/estrellas/estrellas';
import { IconoComponent } from '../compartido/icono/icono';
import { AsignacionService } from '../asignaciones/asignacion.service';
import { Asignacion } from '../asignaciones/asignacion.model';
import { EmpresaService } from '../empresas/empresa.service';
import { Empresa, esVistaProfesor } from '../empresas/empresa.model';
import { TarjetaEmpresaComponent } from '../empresas/tarjeta-empresa/tarjeta-empresa';
import { ReviewService } from '../reviews/review.service';
import { CalificacionConfig, Review } from '../reviews/review.model';
import { MENSAJES_REVIEW, mensajeDeError } from '../auth/mensajes-error';
import { CabeceraComponent } from '../compartido/cabecera/cabecera';
import { AlertaComponent } from '../compartido/alerta/alerta';
import { CampoComponent } from '../compartido/campo/campo';
import { BotonComponent } from '../compartido/boton/boton';
import { PanelService, ResumenCentro } from './panel.service';

/** Cuántas filas/tarjetas caben en un resumen antes de mandar al listado completo. */
const RESUMEN = 4;

@Component({
  selector: 'app-panel-page',
  imports: [
    IconoComponent,
    EstadoComponent,
    TarjetaEmpresaComponent,
    CarruselComponent,
    EstrellasComponent,
    CabeceraComponent,
    AlertaComponent,
    CampoComponent,
    BotonComponent,
  ],
  templateUrl: './panel-page.html',
})
export class PanelPage {
  private readonly auth = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);
  private readonly reviewService = inject(ReviewService);
  private readonly asignacionService = inject(AsignacionService);
  private readonly panelService = inject(PanelService);

  protected readonly sesion = this.auth.sesion;
  protected readonly esAlumno = computed(() => this.sesion()?.rol === 'ALUMNO');
  protected readonly titulo = computed(() => {
    if (!this.esAlumno()) return 'Panel del centro';
    const correo = this.sesion()?.correo;
    return correo ? `Hola, ${correo}` : 'Hola';
  });

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly empresas = signal<Empresa[]>([]);
  protected readonly pendientes = signal<Review[]>([]);
  protected readonly asignaciones = signal<Asignacion[]>([]);
  protected readonly resumen = signal<ResumenCentro | null>(null);
  /** El rango lo fija cada instituto; sin él no se pueden pintar las estrellas. */
  protected readonly calificacion = signal<CalificacionConfig | null>(null);
  protected readonly moderandoId = signal<number | null>(null);
  protected readonly rechazandoId = signal<number | null>(null);
  protected readonly errorModeracion = signal<{ id: number; mensaje: string } | null>(null);

  /**
   * El carrusel solo enseña empresas publicadas. Al alumnado el backend ya le
   * manda solo esas (su DTO ni trae `publicada`); al profesorado le llegan
   * todas, y las que están sin publicar son trabajo a medias, no escaparate.
   */
  protected readonly empresasCarrusel = computed(() =>
    this.empresas().filter((empresa) => !esVistaProfesor(empresa) || empresa.publicada),
  );
  protected readonly pendientesResumen = computed(() => this.pendientes().slice(0, RESUMEN));
  /** La asignación abierta es la que el alumnado ve como "mi empresa". */
  protected readonly asignacionAbierta = computed(() =>
    this.asignaciones().find((asignacion) => asignacion.fechaFin === null),
  );

  constructor() {
    void this.cargar();
  }

  /** El nombre de la empresa de una reseña sale del listado que ya está cargado. */
  protected nombreEmpresa(review: Review): string {
    return this.empresas().find((empresa) => empresa.id === review.empresaId)?.nombre ?? 'Empresa';
  }

  protected async aprobar(review: Review): Promise<void> {
    await this.moderar(review, 'APROBADA', null);
  }

  protected async rechazar(review: Review, motivoRechazo: string): Promise<void> {
    if (!motivoRechazo.trim()) {
      this.errorModeracion.set({ id: review.id, mensaje: 'Indica un motivo de rechazo.' });
      return;
    }
    await this.moderar(review, 'RECHAZADA', motivoRechazo);
  }

  private async moderar(
    review: Review,
    estado: 'APROBADA' | 'RECHAZADA',
    motivoRechazo: string | null,
  ): Promise<void> {
    if (this.moderandoId() !== null) {
      return;
    }
    this.moderandoId.set(review.id);
    this.errorModeracion.set(null);
    try {
      await this.reviewService.moderar(review.id, { estado, motivoRechazo });
      this.pendientes.update((lista) => lista.filter((r) => r.id !== review.id));
      this.rechazandoId.set(null);
    } catch (e) {
      this.errorModeracion.set({ id: review.id, mensaje: mensajeDeError(e, MENSAJES_REVIEW) });
    } finally {
      this.moderandoId.set(null);
    }
  }

  private async cargar(): Promise<void> {
    const sesion = this.sesion();
    try {
      this.empresas.set(await this.empresaService.listar());
      if (sesion?.rol === 'ALUMNO') {
        // Tras un login el id llega null y no se rehidrata hasta el primer /me;
        // sin id no hay a quién pedirle las asignaciones.
        if (sesion.id !== null) {
          this.asignaciones.set(await this.asignacionService.listarPorAlumno(sesion.id));
        }
      } else {
        // Solo el profesorado: al alumnado el resumen le da 403 y ni lo pinta.
        // Y si falla, se queda sin bloque de contadores, pero el resto del
        // panel (empresas, reseñas) tiene que seguir cargando igual.
        try {
          this.resumen.set(await this.panelService.resumen());
        } catch {
          this.resumen.set(null);
        }
        this.pendientes.set(await this.reviewService.listarPendientes());
        this.calificacion.set(await this.reviewService.calificacionConfig());
      }
    } catch {
      this.error.set('No se pudo cargar el panel.');
    } finally {
      this.cargando.set(false);
    }
  }
}
