import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MENSAJES_AFINIDAD, mensajeDeError } from '../../auth/mensajes-error';
import { AfinidadService } from '../afinidad.service';
import { AfinidadEmpresa } from '../afinidad.model';

/**
 * Página única para las dos rutas del contrato de afinidad: autoservicio
 * (`/mi-afinidad`, sin parámetro) y vista de profesor/admin
 * (`/alumnos/:alumnoId/afinidad`) — el shape de respuesta es idéntico, solo
 * cambia qué endpoint se llama.
 */
@Component({
  selector: 'app-afinidad-page',
  imports: [RouterLink],
  templateUrl: './afinidad-page.html',
})
export class AfinidadPage {
  private readonly route = inject(ActivatedRoute);
  private readonly afinidadService = inject(AfinidadService);

  protected readonly alumnoId = this.route.snapshot.paramMap.get('alumnoId');

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly alumnoConEtiquetas = signal(true);
  protected readonly empresas = signal<AfinidadEmpresa[]>([]);

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      const listado = this.alumnoId
        ? await this.afinidadService.deAlumno(Number(this.alumnoId))
        : await this.afinidadService.propia();
      this.alumnoConEtiquetas.set(listado.alumnoConEtiquetas);
      this.empresas.set(listado.empresas);
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_AFINIDAD));
    } finally {
      this.cargando.set(false);
    }
  }
}
