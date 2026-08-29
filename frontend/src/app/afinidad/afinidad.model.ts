import { Empresa, Etiqueta } from '../empresas/empresa.model';

/** Afinidad de un alumno con una empresa, con la explicabilidad del score. */
export interface AfinidadEmpresa {
  empresa: Empresa;
  score: number;
  etiquetasCoincidentes: Etiqueta[];
  sectorCoincide: boolean;
}

/** Listado de afinidad, ya ordenado por el backend (score descendente, empate alfabético). */
export interface AfinidadListado {
  alumnoConEtiquetas: boolean;
  empresas: AfinidadEmpresa[];
}
