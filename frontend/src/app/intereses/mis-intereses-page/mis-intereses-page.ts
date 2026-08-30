import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconoComponent } from '../../compartido/icono/icono';
import { AuthService, Sesion } from '../../auth/auth.service';
import { InteresService } from '../interes.service';
import { Interes } from '../interes.model';

@Component({
  selector: 'app-mis-intereses-page',
  imports: [RouterLink, IconoComponent],
  templateUrl: './mis-intereses-page.html',
})
export class MisInteresesPage {
  private readonly authService = inject(AuthService);
  private readonly interesService = inject(InteresService);

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly intereses = signal<Interes[]>([]);

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      const sesion = await this.completarSesionSiHaceFalta();
      if (sesion === null || sesion.id === null) {
        throw new Error('Sesión sin id');
      }
      this.intereses.set(await this.interesService.listarPorAlumno(sesion.id));
    } catch {
      this.error.set('No se pudieron cargar tus intereses.');
    } finally {
      this.cargando.set(false);
    }
  }

  /**
   * Tras un login sin recargar la página, la sesión en memoria no trae
   * id/correo (asimetría documentada de LoginResponse) — mismo patrón
   * puntual que empresa-detalle-page.ts, sin extraerlo todavía.
   */
  private async completarSesionSiHaceFalta(): Promise<Sesion | null> {
    const sesion = this.authService.sesion();
    if (sesion && sesion.correo === null) {
      return this.authService.me();
    }
    return sesion;
  }
}
