import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { CentroService } from '../centro/centro.service';

/**
 * Compone el título de la pestaña como «<nombre del centro> — <lo que diga la
 * ruta>». Un solo sitio en vez de repetir el prefijo en cada entrada de
 * `app.routes.ts`. `CentroService.centro()` arranca en `'Practikalia'` y no se
 * queda nunca en blanco mientras la petición del centro está en vuelo.
 */
@Injectable({ providedIn: 'root' })
export class TituloPractikalia extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly centro = inject(CentroService);

  override updateTitle(estado: RouterStateSnapshot): void {
    const titulo = this.buildTitle(estado);
    const nombre = this.centro.centro().nombre;
    this.title.setTitle(titulo ? `${nombre} — ${titulo}` : nombre);
  }
}
