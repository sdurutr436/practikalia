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
