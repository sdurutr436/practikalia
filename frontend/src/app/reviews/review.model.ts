export type EstadoReview = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';

/** Review de una asignación concreta — alumno/empresa se derivan de ella en el backend. */
export interface Review {
  id: number;
  asignacionId: number;
  empresaId: number;
  alumnoCorreo: string;
  autorCorreo: string;
  contenido: string;
  calificacion: number;
  estado: EstadoReview;
  /** `null` mientras está PENDIENTE. */
  moderadaPorCorreo: string | null;
  /** Solo tiene valor si `estado === 'RECHAZADA'`. */
  motivoRechazo: string | null;
  fechaCreacion: string;
  fechaModeracion: string | null;
}

export interface CrearReviewRequest {
  asignacionId: number;
  contenido: string;
  calificacion: number;
}

export interface EditarReviewRequest {
  contenido: string;
  calificacion: number;
}

export interface ModerarReviewRequest {
  estado: 'APROBADA' | 'RECHAZADA';
  motivoRechazo: string | null;
}

/** Rango de calificación válido, configurable por instituto — nunca hardcodear. */
export interface CalificacionConfig {
  min: number;
  max: number;
}
