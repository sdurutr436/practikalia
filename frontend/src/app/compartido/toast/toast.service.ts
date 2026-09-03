import { Injectable, signal } from '@angular/core';

/** Lo que se queda en pantalla antes de irse solo. */
const DURACION = 5000;

/**
 * Los avisos de «hecho» de la aplicación. Van a una esquina y se van solos, en
 * vez de empujar el contenido de la pantalla hacia abajo como hacía la alerta
 * en línea: lo que se acaba de guardar ya está a la vista, el aviso solo lo
 * confirma.
 *
 * ponytail: un aviso a la vez, sin cola. Todos salen de una acción que acaba
 * de hacer una persona, y no da tiempo a encadenar dos.
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly mensaje = signal<string | null>(null);
  private temporizador?: ReturnType<typeof setTimeout>;

  mostrar(mensaje: string): void {
    clearTimeout(this.temporizador);
    this.mensaje.set(mensaje);
    this.temporizador = setTimeout(() => this.mensaje.set(null), DURACION);
  }

  cerrar(): void {
    clearTimeout(this.temporizador);
    this.mensaje.set(null);
  }
}
