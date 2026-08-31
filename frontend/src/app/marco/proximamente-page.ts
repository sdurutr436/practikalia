import { Component } from '@angular/core';

/**
 * Destino de las secciones del menú que todavía no tienen pantalla. Una sola
 * página para todas: las rutas ya existen, así que crear cada pantalla es
 * cambiar su `component` en app.routes.ts y nada más.
 */
@Component({
  selector: 'app-proximamente-page',
  template: `
    <main class="o-pagina">
      <header class="c-cabecera">
        <h1 class="c-cabecera__titulo">Próximamente</h1>
      </header>
      <p class="c-alerta c-alerta--aviso" role="status">
        Esta sección todavía no está construida. El acceso ya está en el menú para cuando lo esté.
      </p>
    </main>
  `,
})
export class ProximamentePage {}
