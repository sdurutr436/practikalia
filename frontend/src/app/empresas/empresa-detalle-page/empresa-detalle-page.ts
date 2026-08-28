import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MENSAJES_ASIGNACION, mensajeDeError } from '../../auth/mensajes-error';
import { AsignacionService } from '../../asignaciones/asignacion.service';
import { Asignacion } from '../../asignaciones/asignacion.model';
import { AuthService } from '../../auth/auth.service';
import { ReviewService } from '../../reviews/review.service';
import { Review } from '../../reviews/review.model';
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
      }
      void this.cargarReviews(id);
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
