/** Forma común de las respuestas paginadas del backend (`PaginaDto`). */
interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  total: number;
  paginas: number;
}

/** Envuelve un listado en la página que devuelven `GET /api/empresas` y `GET /api/reviews`. */
export function pagina<T>(contenido: T[], extra: Partial<Pagina<T>> = {}): Pagina<T> {
  return {
    contenido,
    pagina: 0,
    tamano: contenido.length,
    total: contenido.length,
    paginas: 1,
    ...extra,
  };
}
