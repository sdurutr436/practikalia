import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Una pastilla: la clave que viaja en la URL y lo que se lee en pantalla. */
export interface Pastilla {
  clave: string;
  etiqueta: string;
}

/**
 * Fila de pastillas de filtro de un listado. Estaba copiada en empresas,
 * reseñas, alumnado y asignaciones, y con ella el detalle fácil de olvidar:
 * cambiar de pastilla tiene que volver a la primera página.
 *
 * Son enlaces y no botones a propósito — el filtro vive en la URL, así que se
 * comparte, se abre en otra pestaña y el botón de atrás funciona.
 */
@Component({
  selector: 'app-pastillas',
  imports: [RouterLink],
  templateUrl: './pastillas.html',
  host: { class: 'u-contenidos' },
})
export class PastillasComponent {
  readonly opciones = input.required<readonly Pastilla[]>();
  /** Clave de la pastilla activa. La de clave vacía es el «todas» sin parámetro. */
  readonly activa = input.required<string>();
  readonly ruta = input.required<string>();
  /** Parámetro de la URL por el que viaja la clave. */
  readonly parametro = input('estado');
  /** Otros parámetros que deja de tener sentido conservar al cambiar de pastilla. */
  readonly limpiar = input<readonly string[]>([]);

  protected parametros(pastilla: Pastilla): Record<string, string | null> {
    const parametros: Record<string, string | null> = {
      [this.parametro()]: pastilla.clave || null,
      pagina: null,
    };
    for (const clave of this.limpiar()) {
      parametros[clave] = null;
    }
    return parametros;
  }
}
