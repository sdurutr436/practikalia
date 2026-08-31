/** Catálogo de grados/ciclos, sin más gestión que lectura (mismo patrón que Etiqueta). */
export interface Grado {
  id: number;
  nombre: string;
}

/** Vista mínima de usuario para los selects de alumno/tutor, sin datos sensibles. */
export interface UsuarioResumen {
  id: number;
  correo: string;
  rol: 'ALUMNO' | 'PROFESOR';
}

/** Asignación de un alumno a una empresa, con el grado/año snapshot de cuando se creó. */
export interface Asignacion {
  id: number;
  alumnoId: number;
  alumnoCorreo: string;
  empresaId: number;
  empresaNombre: string;
  tutorCentroId: number;
  tutorCentroCorreo: string;
  grado: Grado;
  anio: number;
  fechaInicio: string;
  /** `null` mientras la asignación sigue abierta. */
  fechaFin: string | null;
  /** `null` = sin dato informado todavía; solo tiene sentido una vez cerrada. */
  contratadoPosterior: boolean | null;
}

/**
 * Texto de la contratación posterior. Vive aquí porque la ficha de empresa y
 * el histórico del alumno pintaban el mismo ternario de tres ramas en plantilla.
 */
export function textoContratacion(asignacion: Asignacion): string {
  if (asignacion.contratadoPosterior === null) return 'sin decidir';
  return asignacion.contratadoPosterior ? 'sí' : 'no';
}

export interface CrearAsignacionRequest {
  alumnoId: number;
  empresaId: number;
  tutorCentroId: number;
  gradoId: number;
  anio: number;
  fechaInicio: string;
}

export interface ActualizarAsignacionRequest {
  fechaFin: string;
  contratadoPosterior: boolean | null;
}

export interface ActualizarGradoRequest {
  gradoId: number;
  anio: number;
}

/** Grado/año fijado a un alumno — respuesta de PUT /api/usuarios/{id}/grado. */
export interface UsuarioGrado {
  id: number;
  correo: string;
  grado: Grado;
  anio: number;
}

/**
 * Tasa de contratación de una empresa. `tasa` es un ratio 0.0–1.0, no un
 * porcentaje. `asignacionesDecididas === 0` significa "sin datos todavía",
 * no "0% de contratación real".
 */
export interface TasaContratacion {
  empresaId: number;
  asignacionesDecididas: number;
  contrataciones: number;
  tasa: number;
}
