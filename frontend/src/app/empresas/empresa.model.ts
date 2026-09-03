/** Catálogo anidado (sector o etiqueta), sin endpoint de listado propio. */
export interface Etiqueta {
  id: number;
  nombre: string;
}

/**
 * Personal de la empresa que puede tutorizar a un alumno en prácticas. No es
 * una cuenta de la aplicación: es un dato de la ficha de su empresa.
 */
export interface TutorEmpresa {
  /** `null` mientras no se ha guardado. */
  id: number | null;
  nombre: string;
  cargo: string | null;
  telefono: string | null;
  correo: string | null;
}

/**
 * Campos comunes a EmpresaAlumnoDto y EmpresaProfesorDto. Los exclusivos de
 * profesor/admin son opcionales: el backend decide cuáles manda según el rol
 * de quien pregunta, el frontend no lo asume por su cuenta.
 */
export interface Empresa {
  id: number;
  nombre: string;
  descripcion: string | null;
  imagen: string | null;
  direccion: string | null;
  sector: Etiqueta;
  etiquetas: Etiqueta[];
  observaciones?: string | null;
  tutores?: TutorEmpresa[];
  contactoNombre?: string | null;
  contactoTelefono?: string | null;
  contactoEmail?: string | null;
  publicada?: boolean;
  creadaPorCorreo?: string;
  fechaCreacion?: string;
}

/** true si la respuesta trae los campos exclusivos de EmpresaProfesorDto. */
export function esVistaProfesor(empresa: Empresa): empresa is Required<Empresa> {
  return empresa.publicada !== undefined;
}

/** Página de empresas que devuelve `GET /api/empresas`. */
export interface PaginaEmpresas {
  contenido: Empresa[];
  pagina: number;
  tamano: number;
  total: number;
  paginas: number;
}

/** Criterios del listado; todos opcionales. Sin `tamano`, el catálogo entero. */
export interface ConsultaEmpresas {
  texto?: string | null;
  publicada?: boolean | null;
  sectorId?: number | null;
  etiquetaIds?: number[];
  pagina?: number;
  tamano?: number;
}

/** Mismo shape para crear (POST) y actualizar (PUT) — igual que el backend. */
export interface EmpresaRequest {
  nombre: string;
  descripcion: string;
  direccion: string;
  sectorId: number;
  etiquetaIds: number[];
  observaciones: string;
  /** Reemplazo completo: los que falten se borran. Hace falta al menos uno. */
  tutores: TutorEmpresa[];
  contactoNombre: string;
  contactoTelefono: string;
  contactoEmail: string;
  publicada: boolean;
}
