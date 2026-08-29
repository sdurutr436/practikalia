import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MENSAJES_ASIGNACION, MENSAJES_INTERES, mensajeDeError } from '../../auth/mensajes-error';
import { AsignacionService } from '../../asignaciones/asignacion.service';
import { Asignacion } from '../../asignaciones/asignacion.model';
import { AuthService, Sesion } from '../../auth/auth.service';
import { ReviewService } from '../../reviews/review.service';
import { Review } from '../../reviews/review.model';
import { InteresService } from '../../intereses/interes.service';
import { Interesado } from '../../intereses/interes.model';
import { EmpresaService } from '../empresa.service';
import { Empresa, esVistaProfesor } from '../empresa.model';

@Component({
  selector: 'app-empresa-detalle-page',
  imports: [RouterLink],
  templateUrl: './empresa-detalle-page.html',
})
export class EmpresaDetallePage {
  private readonly route = inject(ActivatedRoute);
  private readonly empresaService = inject(EmpresaService);
  private readonly asignacionService = inject(AsignacionService);
  private readonly reviewService = inject(ReviewService);
  private readonly interesService = inject(InteresService);
  private readonly authService = inject(AuthService);

  protected readonly esVistaProfesor = esVistaProfesor;
  protected readonly sesion = this.authService.sesion;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly empresa = signal<Empresa | null>(null);

  protected readonly asignaciones = signal<Asignacion[]>([]);
  protected readonly cargandoAsignaciones = signal(false);
  protected readonly errorAsignaciones = signal<string | null>(null);
  protected readonly guardandoId = signal<number | null>(null);
  protected readonly errorCierre = signal<{ id: number; mensaje: string } | null>(null);

  protected readonly reviews = signal<Review[]>([]);
  protected readonly cargandoReviews = signal(false);
  protected readonly errorReviews = signal<string | null>(null);
  protected readonly asignacionesSinReview = signal<Asignacion[]>([]);

  protected readonly interesado = signal(false);
  protected readonly guardandoInteres = signal(false);
  protected readonly errorInteres = signal<string | null>(null);

  protected readonly interesados = signal<Interesado[]>([]);
  protected readonly cargandoInteresados = signal(false);
  protected readonly errorInteresados = signal<string | null>(null);

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    try {
      const empresa = await this.empresaService.obtener(id);
      this.empresa.set(empresa);
      if (esVistaProfesor(empresa)) {
        void this.cargarAsignaciones(id);
        void this.cargarInteresados(id);
      }
      const promesaReviews = this.cargarReviews(id);
      const sesion = await this.completarSesionSiHaceFalta();

      if (!esVistaProfesor(empresa) && sesion?.rol === 'ALUMNO' && sesion.id !== null) {
        const alumnoId = sesion.id;
        void promesaReviews.then(() => this.cargarAsignacionesPropiasSinReview(id, alumnoId));
        void this.cargarEstadoInteres(id, alumnoId);
      }
    } catch (e) {
      this.error.set(
        e instanceof HttpErrorResponse && e.status === 404
          ? 'Esta empresa no existe, o no está publicada.'
          : 'No se pudo cargar la empresa.',
      );
    } finally {
      this.cargando.set(false);
    }
  }

  /**
   * Tras un login sin recargar la página, la sesión en memoria no trae
   * id/correo (asimetría documentada de LoginResponse) — se completan aquí
   * bajo demanda, una sola vez por sesión de app, para poder comparar
   * autoría de reviews y cruzar asignaciones propias.
   */
  private async completarSesionSiHaceFalta(): Promise<Sesion | null> {
    const sesion = this.authService.sesion();
    if (sesion && sesion.correo === null) {
      return this.authService.me();
    }
    return sesion;
  }

  private async cargarReviews(empresaId: number): Promise<void> {
    this.cargandoReviews.set(true);
    try {
      this.reviews.set(await this.reviewService.listarPorEmpresa(empresaId));
    } catch {
      this.errorReviews.set('No se pudieron cargar las reviews.');
    } finally {
      this.cargandoReviews.set(false);
    }
  }

  /** Cruza las asignaciones propias del alumno en esta empresa contra las reviews ya cargadas. */
  private async cargarAsignacionesPropiasSinReview(empresaId: number, alumnoId: number): Promise<void> {
    try {
      const propias = await this.asignacionService.listarPorAlumno(alumnoId);
      const conReview = new Set(this.reviews().map((r) => r.asignacionId));
      this.asignacionesSinReview.set(propias.filter((a) => a.empresaId === empresaId && !conReview.has(a.id)));
    } catch {
      // ponytail: best-effort — si falla, simplemente no se ofrece el atajo de "escribir review" aquí.
    }
  }

  private async cargarInteresados(empresaId: number): Promise<void> {
    this.cargandoInteresados.set(true);
    try {
      this.interesados.set(await this.interesService.listarInteresados(empresaId));
    } catch {
      this.errorInteresados.set('No se pudo cargar la lista de interesados.');
    } finally {
      this.cargandoInteresados.set(false);
    }
  }

  private async cargarEstadoInteres(empresaId: number, alumnoId: number): Promise<void> {
    try {
      const intereses = await this.interesService.listarPorAlumno(alumnoId);
      this.interesado.set(intereses.some((i) => i.empresaId === empresaId));
    } catch {
      // ponytail: best-effort — si falla, el botón queda en su estado inicial "no interesado".
    }
  }

  protected async alternarInteres(empresaId: number): Promise<void> {
    if (this.guardandoInteres()) {
      return;
    }
    this.guardandoInteres.set(true);
    this.errorInteres.set(null);
    try {
      if (this.interesado()) {
        await this.interesService.desmarcar(empresaId);
        this.interesado.set(false);
      } else {
        await this.interesService.marcar(empresaId);
        this.interesado.set(true);
      }
    } catch (e) {
      this.errorInteres.set(mensajeDeError(e, MENSAJES_INTERES));
    } finally {
      this.guardandoInteres.set(false);
    }
  }

  private async cargarAsignaciones(empresaId: number): Promise<void> {
    this.cargandoAsignaciones.set(true);
    try {
      this.asignaciones.set(await this.asignacionService.listarPorEmpresa(empresaId));
    } catch {
      this.errorAsignaciones.set('No se pudieron cargar las asignaciones.');
    } finally {
      this.cargandoAsignaciones.set(false);
    }
  }

  protected async cerrarAsignacion(asignacion: Asignacion, fechaFin: string, contratadoTexto: string): Promise<void> {
    if (!fechaFin || this.guardandoId() !== null) {
      return;
    }
    this.guardandoId.set(asignacion.id);
    this.errorCierre.set(null);
    const contratadoPosterior = contratadoTexto === '' ? null : contratadoTexto === 'true';
    try {
      const actualizada = await this.asignacionService.cerrar(asignacion.id, { fechaFin, contratadoPosterior });
      this.asignaciones.update((lista) => lista.map((a) => (a.id === actualizada.id ? actualizada : a)));
    } catch (e) {
      this.errorCierre.set({ id: asignacion.id, mensaje: mensajeDeError(e, MENSAJES_ASIGNACION) });
    } finally {
      this.guardandoId.set(null);
    }
  }
}
