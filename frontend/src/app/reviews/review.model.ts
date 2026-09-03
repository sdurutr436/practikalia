export type EstadoReview = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';

/** Review de una asignación concreta — alumno/empresa se derivan de ella en el backend. */
export interface Review {
  id: number;
  asignacionId: number;
  empresaId: number;
  empresaNombre: string;
  alumnoCorreo: string;
  /** `null` si la cuenta no tiene nombre: las creadas por un profesor no lo piden. */
  alumnoNombre: string | null;
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

/** Una página de la cola de moderación (`GET /api/reviews`). */
export interface PaginaReviews {
  contenido: Review[];
  pagina: number;
  tamano: number;
  total: number;
  paginas: number;
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
