import { Component } from '@angular/core';

/**
 * Fondo de figuras en movimiento, detrás de las tarjetas y los menús de una
 * página. Se recorta a sí mismo, así que basta con que el contenedor donde se
 * suelta tenga `position: relative`.
 *
 * Sin entradas a propósito: la gracia es que todas las pantallas compartan la
 * misma sopa. Si alguna necesita otra densidad, se añade el input entonces.
 */
@Component({
  selector: 'app-fondo',
  templateUrl: './fondo.html',
})
export class FondoComponent {}
