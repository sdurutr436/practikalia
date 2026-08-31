import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { MarcoComponent } from './marco/marco';

/** Rutas que se pintan a pantalla completa, sin menú ni barra superior. */
const SIN_MARCO = ['/login', '/cambiar-contrasena'];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MarcoComponent],
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);

  protected readonly conMarco = signal(false);

  constructor() {
    this.router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() =>
        this.conMarco.set(!SIN_MARCO.some((ruta) => this.router.url.startsWith(ruta))),
      );
  }
}
