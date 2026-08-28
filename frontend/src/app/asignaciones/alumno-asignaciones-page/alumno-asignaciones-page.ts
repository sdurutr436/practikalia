import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AsignacionService } from '../asignacion.service';
import { Asignacion } from '../asignacion.model';

@Component({
  selector: 'app-alumno-asignaciones-page',
  imports: [RouterLink],
  templateUrl: './alumno-asignaciones-page.html',
})
export class AlumnoAsignacionesPage {
  private readonly route = inject(ActivatedRoute);
  private readonly asignacionService = inject(AsignacionService);

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly asignaciones = signal<Asignacion[]>([]);

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    const alumnoId = Number(this.route.snapshot.paramMap.get('alumnoId'));
    try {
      this.asignaciones.set(await this.asignacionService.listarPorAlumno(alumnoId));
    } catch {
      this.error.set('No se pudo cargar el histórico del alumno.');
    } finally {
      this.cargando.set(false);
    }
  }
}
