import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EstadoComponent } from '../../compartido/estado/estado';
import { AuthService } from '../../auth/auth.service';
import { InteresService } from '../interes.service';
import { Interes } from '../interes.model';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { VolverComponent } from '../../compartido/volver/volver';

@Component({
  selector: 'app-mis-intereses-page',
  imports: [RouterLink, EstadoComponent, CabeceraComponent, VolverComponent],
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
      const sesion = await this.authService.completarSesionSiHaceFalta();
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
}
