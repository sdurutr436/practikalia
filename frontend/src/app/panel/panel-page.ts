import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { EstadoComponent } from '../compartido/estado/estado';
import { IconoComponent } from '../compartido/icono/icono';
import { AsignacionService } from '../asignaciones/asignacion.service';
import { Asignacion } from '../asignaciones/asignacion.model';
import { EmpresaService } from '../empresas/empresa.service';
import { Empresa, esVistaProfesor } from '../empresas/empresa.model';
import { TarjetaEmpresaComponent } from '../empresas/tarjeta-empresa/tarjeta-empresa';
import { ReviewService } from '../reviews/review.service';
import { Review } from '../reviews/review.model';

/** Cuántas filas/tarjetas caben en un resumen antes de mandar al listado completo. */
const RESUMEN = 4;

@Component({
  selector: 'app-panel-page',
  imports: [RouterLink, IconoComponent, EstadoComponent, TarjetaEmpresaComponent],
  templateUrl: './panel-page.html',
})
export class PanelPage {
  private readonly auth = inject(AuthService);
  private readonly empresaService = inject(EmpresaService);
  private readonly reviewService = inject(ReviewService);
  private readonly asignacionService = inject(AsignacionService);

  protected readonly esVistaProfesor = esVistaProfesor;
  protected readonly sesion = this.auth.sesion;
  protected readonly esAlumno = computed(() => this.sesion()?.rol === 'ALUMNO');

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly empresas = signal<Empresa[]>([]);
  protected readonly pendientes = signal<Review[]>([]);
  protected readonly asignaciones = signal<Asignacion[]>([]);

  protected readonly empresasResumen = computed(() => this.empresas().slice(0, RESUMEN));
  protected readonly pendientesResumen = computed(() => this.pendientes().slice(0, RESUMEN));
  /** La asignación abierta es la que el alumnado ve como "mi empresa". */
  protected readonly asignacionAbierta = computed(() =>
    this.asignaciones().find((asignacion) => asignacion.fechaFin === null),
  );

  constructor() {
    void this.cargar();
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
        this.pendientes.set(await this.reviewService.listarPendientes());
      }
    } catch {
      this.error.set('No se pudo cargar el panel.');
    } finally {
      this.cargando.set(false);
    }
  }
}
