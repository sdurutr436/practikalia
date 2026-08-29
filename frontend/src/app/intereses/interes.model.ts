/** Interés de un alumno en una empresa, con el grado/año snapshot de cuando se marcó. */
export interface Interes {
  empresaId: number;
  empresaNombre: string;
  gradoNombre: string;
  anio: number;
  fechaCreacion: string;
}

/** Un alumno interesado en una empresa, con el grado/año snapshot de cuando marcó el interés. */
export interface Interesado {
  alumnoId: number;
  alumnoCorreo: string;
  gradoNombre: string;
  anio: number;
  fechaCreacion: string;
}
