import { Empresa, PaginaEmpresas } from './empresas/empresa.model';

/** Envuelve un listado en la página que devuelve `GET /api/empresas`. */
export function pagina(contenido: Empresa[], extra: Partial<PaginaEmpresas> = {}): PaginaEmpresas {
  return {
    contenido,
    pagina: 0,
    tamano: contenido.length,
    total: contenido.length,
    paginas: 1,
    ...extra,
  };
}
