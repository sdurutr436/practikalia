import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AsignacionService } from '../asignacion.service';
import { Asignacion } from '../asignacion.model';
import { ReviewService } from '../../reviews/review.service';

@Component({
  selector: 'app-alumno-asignaciones-page',
  imports: [RouterLink],
  templateUrl: './alumno-asignaciones-page.html',
})
export class AlumnoAsignacionesPage {
  private readonly route = inject(ActivatedRoute);
  private readonly asignacionService = inject(AsignacionService);
  private readonly reviewService = inject(ReviewService);

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly asignaciones = signal<Asignacion[]>([]);
  protected readonly asignacionesConReview = signal<Set<number>>(new Set());

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    const alumnoId = Number(this.route.snapshot.paramMap.get('alumnoId'));
    try {
      const asignaciones = await this.asignacionService.listarPorAlumno(alumnoId);
      this.asignaciones.set(asignaciones);
      await this.cargarReviewsExistentes(asignaciones);
    } catch {
      this.error.set('No se pudo cargar el histórico del alumno.');
    } finally {
      this.cargando.set(false);
    }
  }

  private async cargarReviewsExistentes(asignaciones: Asignacion[]): Promise<void> {
    try {
      const empresaIds = [...new Set(asignaciones.map((a) => a.empresaId))];
      const listas = await Promise.all(empresaIds.map((id) => this.reviewService.listarPorEmpresa(id)));
      this.asignacionesConReview.set(new Set(listas.flat().map((r) => r.asignacionId)));
    } catch {
      // ponytail: best-effort — si falla, se ofrece el atajo de más y el backend igual protege con 409.
    }
  }
}
