import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/**
 * Página general PROVISIONAL: destino tras el login mientras no existen las
 * pantallas de dominio (F2+). Se sustituye por el shell real de navegación.
 */
@Component({
  selector: 'app-general-page',
  imports: [RouterLink],
  templateUrl: './general-page.html',
})
export class GeneralPage {
  // ponytail: cierra la sesión nada más cargar (decisión: es lo más simple —
  // un único punto de cierre; breadcrumb y botón son enlaces puros a /login).
  // Así "volver atrás" nunca recupera una sesión viva.
  constructor() {
    void inject(AuthService).logout();
  }
}
