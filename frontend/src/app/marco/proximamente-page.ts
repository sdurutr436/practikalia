import { Component } from '@angular/core';
import { AlertaComponent } from '../compartido/alerta/alerta';
import { CabeceraComponent } from '../compartido/cabecera/cabecera';

/**
 * Destino de las secciones del menú que todavía no tienen pantalla. Una sola
 * página para todas: las rutas ya existen, así que crear cada pantalla es
 * cambiar su `component` en app.routes.ts y nada más.
 */
@Component({
  selector: 'app-proximamente-page',
  imports: [CabeceraComponent, AlertaComponent],
  template: `
    <main class="o-pagina">
      <app-cabecera titulo="Próximamente" />
      <app-alerta tipo="aviso">
        Esta sección todavía no está construida. El acceso ya está en el menú para cuando lo esté.
      </app-alerta>
    </main>
  `,
})
export class ProximamentePage {}
