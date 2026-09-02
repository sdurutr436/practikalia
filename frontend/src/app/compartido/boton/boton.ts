import { NgTemplateOutlet } from '@angular/common';
import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconoComponent, NombreIcono } from '../icono/icono';

/**
 * Botón del tema en sus dos formas: `<button>` de acción y `<a>` de navegación
 * (el mockup las pinta igual, de ahí que compartan `.c-boton`). Aparte del
 * markup, unifica el patrón de envío que ocho formularios repetían a mano
 * —deshabilitar mientras se guarda y cambiar el texto—, que es la parte capaz
 * de divergir entre pantallas sin que se note.
 */
@Component({
  selector: 'app-boton',
  imports: [NgTemplateOutlet, RouterLink, IconoComponent],
  templateUrl: './boton.html',
  // El ítem de .c-acciones o de la rejilla de .c-formulario tiene que ser el
  // <button> de verdad, no el anfitrión.
  host: { class: 'u-contenidos' },
})
export class BotonComponent {
  readonly icono = input<NombreIcono>();
  readonly secundario = input(false);
  readonly deshabilitado = input(false);
  /** Envío en curso: deshabilita y sustituye el texto proyectado. */
  readonly cargando = input(false);
  readonly textoCargando = input('Guardando…');
  readonly tipo = input<'button' | 'submit'>('button');
  /** Con destino el botón se pinta como `<a>` y navega en vez de emitir click. */
  readonly enlace = input<string | unknown[]>();
  readonly params = input<Record<string, unknown>>();
}
