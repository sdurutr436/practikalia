import { Injectable, inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/**
 * Compone el título de la pestaña como «Practikalia — <lo que diga la ruta>».
 * Un solo sitio en vez de repetir el prefijo en cada entrada de app.routes.ts,
 * que es lo que habrá que tocar cuando el nombre lo ponga el centro.
 */
@Injectable({ providedIn: 'root' })
export class TituloPractikalia extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(estado: RouterStateSnapshot): void {
    const titulo = this.buildTitle(estado);
    this.title.setTitle(titulo ? `Practikalia — ${titulo}` : 'Practikalia');
  }
}
