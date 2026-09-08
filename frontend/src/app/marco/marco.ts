import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { CentroService } from '../centro/centro.service';
import { FondoComponent } from '../compartido/fondo/fondo';
import { IconoComponent, NombreIcono } from '../compartido/icono/icono';
import { BotonComponent } from '../compartido/boton/boton';
import { AcordeonComponent } from '../compartido/acordeon/acordeon';

interface Seccion {
  etiqueta: string;
  icono: NombreIcono;
  ruta: string;
  /** Solo para quien tenga `esAdmin`; el resto del profesorado ni la ve. */
  soloAdmin?: boolean;
}

/**
 * Menú del profesorado. Configuración es la misma pantalla para todo el
 * mundo; solo el propio formulario se restringe a `esAdmin`, con un aviso para
 * el resto. Sectores y etiquetas sí sale solo con `esAdmin` en el menú.
 * Actividad no sale: la sección no tiene sentido en esta aplicación, aunque su
 * ruta siga respondiendo si se escribe a mano.
 */
const SECCIONES_PROFESOR: Seccion[] = [
  { etiqueta: 'Panel', icono: 'panel', ruta: '/panel' },
  { etiqueta: 'Empresas', icono: 'empresa', ruta: '/empresas' },
  { etiqueta: 'Reseñas', icono: 'moderacion', ruta: '/reviews' },
  { etiqueta: 'Alumnado', icono: 'persona', ruta: '/alumnado' },
  { etiqueta: 'Asignaciones', icono: 'asignaciones', ruta: '/asignaciones' },
  { etiqueta: 'Sectores y etiquetas', icono: 'etiqueta', ruta: '/sectores', soloAdmin: true },
  { etiqueta: 'Profesorado', icono: 'grado', ruta: '/profesorado' },
  { etiqueta: 'Configuración', icono: 'configuracion', ruta: '/configuracion' },
];

/**
 * Menú del alumnado. Además de las secciones del mockup lleva Mis etiquetas y
 * Mi afinidad: son pantallas que ya existen y, al pasar la navegación al
 * marco, este menú es el único sitio desde el que se llega a ellas.
 */
const SECCIONES_ALUMNO: Seccion[] = [
  { etiqueta: 'Panel', icono: 'panel', ruta: '/panel' },
  { etiqueta: 'Empresas', icono: 'empresa', ruta: '/empresas' },
  { etiqueta: 'Reseñas', icono: 'comentario', ruta: '/mis-resenas' },
  { etiqueta: 'Mis intereses', icono: 'interes', ruta: '/mis-intereses' },
  { etiqueta: 'Mi empresa', icono: 'asignaciones', ruta: '/mi-empresa' },
  { etiqueta: 'Mis etiquetas', icono: 'etiqueta', ruta: '/mis-etiquetas' },
  { etiqueta: 'Mi afinidad', icono: 'afinidad', ruta: '/mi-afinidad' },
  { etiqueta: 'Mis documentos', icono: 'nota', ruta: '/mis-documentos' },
  { etiqueta: 'Configuración', icono: 'configuracion', ruta: '/configuracion' },
];

/**
 * Marco de la aplicación: menú lateral, barra superior y fondo de figuras.
 * Envuelve el router-outlet desde app.ts, no como ruta padre: una ruta padre
 * haría que RouterTestingHarness devolviese el marco en vez de la página y
 * obligaría a reescribir media suite de flujos para nada.
 *
 * La página proyectada pone solo su contenido; la navegación de sesión que
 * cada una repetía en su cabecera vive ahora aquí.
 */
@Component({
  selector: 'app-marco',
  imports: [
    RouterLink,
    RouterLinkActive,
    IconoComponent,
    FondoComponent,
    BotonComponent,
    AcordeonComponent,
  ],
  templateUrl: './marco.html',
})
export class MarcoComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly centroService = inject(CentroService);

  protected readonly sesion = this.auth.sesion;
  // Cerrado al entrar: el menú flota sobre la página, y abierto por defecto
  // taparía el contenido nada más cargar.
  protected readonly abierto = signal(false);

  protected readonly secciones = computed(() => {
    const sesion = this.sesion();
    if (sesion?.rol === 'ALUMNO') {
      return SECCIONES_ALUMNO;
    }
    // Enseñar una sección que el guard va a rebotar es peor que no enseñarla.
    return SECCIONES_PROFESOR.filter((seccion) => !seccion.soloAdmin || sesion?.esAdmin);
  });

  protected readonly rolLegible = computed(() =>
    this.sesion()?.rol === 'ALUMNO' ? 'Alumnado' : 'Profesorado',
  );

  protected async cerrarSesion(): Promise<void> {
    await this.auth.logout();
    await this.router.navigate(['/login']);
  }
}
