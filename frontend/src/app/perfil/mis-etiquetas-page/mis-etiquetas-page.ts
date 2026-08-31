import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconoComponent } from '../../compartido/icono/icono';
import { EstadoComponent } from '../../compartido/estado/estado';
import { AuthService, Sesion } from '../../auth/auth.service';
import { MENSAJES_PERFIL, mensajeDeError } from '../../auth/mensajes-error';
import { PerfilService } from '../perfil.service';
import { Etiqueta } from '../../empresas/empresa.model';

@Component({
  selector: 'app-mis-etiquetas-page',
  imports: [RouterLink, IconoComponent, EstadoComponent],
  templateUrl: './mis-etiquetas-page.html',
})
export class MisEtiquetasPage {
  private readonly authService = inject(AuthService);
  private readonly perfilService = inject(PerfilService);

  private alumnoId: number | null = null;

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly catalogo = signal<Etiqueta[]>([]);
  protected readonly seleccionadas = signal<Set<number>>(new Set());
  protected readonly guardando = signal(false);
  protected readonly errorGuardar = signal<string | null>(null);
  protected readonly guardado = signal(false);

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      const sesion = await this.completarSesionSiHaceFalta();
      if (sesion === null || sesion.id === null) {
        throw new Error('Sesión sin id');
      }
      this.alumnoId = sesion.id;
      const [catalogo, actuales] = await Promise.all([
        this.perfilService.listarEtiquetas(),
        this.perfilService.obtenerEtiquetas(sesion.id),
      ]);
      this.catalogo.set(catalogo);
      this.seleccionadas.set(new Set(actuales.map((e) => e.id)));
    } catch {
      this.error.set('No se pudieron cargar tus etiquetas.');
    } finally {
      this.cargando.set(false);
    }
  }

  /**
   * Tras un login sin recargar la página, la sesión en memoria no trae
   * id/correo (asimetría documentada de LoginResponse) — mismo patrón
   * puntual que mis-intereses-page.ts, sin extraerlo todavía.
   */
  private async completarSesionSiHaceFalta(): Promise<Sesion | null> {
    const sesion = this.authService.sesion();
    if (sesion && sesion.correo === null) {
      return this.authService.me();
    }
    return sesion;
  }

  protected toggle(id: number, marcada: boolean): void {
    const seleccion = new Set(this.seleccionadas());
    if (marcada) {
      seleccion.add(id);
    } else {
      seleccion.delete(id);
    }
    this.seleccionadas.set(seleccion);
  }

  protected async guardar(): Promise<void> {
    if (this.guardando() || this.alumnoId === null) {
      return;
    }
    this.guardando.set(true);
    this.errorGuardar.set(null);
    this.guardado.set(false);
    try {
      const actualizadas = await this.perfilService.actualizarEtiquetas(this.alumnoId, [
        ...this.seleccionadas(),
      ]);
      this.seleccionadas.set(new Set(actualizadas.map((e) => e.id)));
      this.guardado.set(true);
    } catch (e) {
      this.errorGuardar.set(mensajeDeError(e, MENSAJES_PERFIL));
    } finally {
      this.guardando.set(false);
    }
  }
}
