/** Catálogo anidado (sector o etiqueta), sin endpoint de listado propio. */
export interface Etiqueta {
  id: number;
  nombre: string;
}

/**
 * Campos comunes a EmpresaAlumnoDto y EmpresaProfesorDto. Los exclusivos de
 * profesor/admin son opcionales: el backend decide cuáles manda según el rol
 * de quien pregunta, el frontend no lo asume por su cuenta.
 */
export interface Empresa {
  id: number;
  nombre: string;
  descripcion: string;
  imagen: string | null;
  direccion: string;
  sector: Etiqueta;
  etiquetas: Etiqueta[];
  observaciones?: string;
  contactoNombre?: string;
  contactoTelefono?: string;
  contactoEmail?: string;
  publicada?: boolean;
  creadaPorCorreo?: string;
  fechaCreacion?: string;
}

/** true si la respuesta trae los campos exclusivos de EmpresaProfesorDto. */
export function esVistaProfesor(empresa: Empresa): empresa is Required<Empresa> {
  return empresa.publicada !== undefined;
}

/** Mismo shape para crear (POST) y actualizar (PUT) — igual que el backend. */
export interface EmpresaRequest {
  nombre: string;
  descripcion: string;
  direccion: string;
  sectorId: number;
  etiquetaIds: number[];
  observaciones: string;
  contactoNombre: string;
  contactoTelefono: string;
  contactoEmail: string;
  publicada: boolean;
}
