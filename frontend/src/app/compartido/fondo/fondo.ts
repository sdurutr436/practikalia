import { Component } from '@angular/core';

/**
 * Fondo de figuras en movimiento, detrás de las tarjetas y los menús de una
 * página. Se recorta a sí mismo, así que basta con que el contenedor donde se
 * suelta tenga `position: relative`.
 *
 * `.c-fondo` va en el propio anfitrión, no en un <div> interno: el anfitrión
 * es `display: inline` por defecto y, dentro de un contenedor `grid`, sería un
 * ítem más de la rejilla ocupando su celda y empujando al resto. Con la clase
 * encima queda `position: absolute` y fuera del flujo, que es lo que toca.
 *
 * Sin entradas a propósito: la gracia es que todas las pantallas compartan la
 * misma sopa. Si alguna necesita otra densidad, se añade el input entonces.
 */
@Component({
  selector: 'app-fondo',
  templateUrl: './fondo.html',
  host: { class: 'c-fondo', 'aria-hidden': 'true' },
})
export class FondoComponent {}
