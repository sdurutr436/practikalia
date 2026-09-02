import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  CalificacionConfig,
  CrearReviewRequest,
  EditarReviewRequest,
  EstadoReview,
  ModerarReviewRequest,
  PaginaReviews,
  Review,
} from './review.model';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);

  calificacionConfig(): Promise<CalificacionConfig> {
    return firstValueFrom(this.http.get<CalificacionConfig>('/api/reviews/calificacion-config'));
  }

  listarPorEmpresa(empresaId: number): Promise<Review[]> {
    return firstValueFrom(this.http.get<Review[]>(`/api/empresas/${empresaId}/reviews`));
  }

  crear(request: CrearReviewRequest): Promise<Review> {
    return firstValueFrom(this.http.post<Review>('/api/reviews', request));
  }

  editar(id: number, request: EditarReviewRequest): Promise<Review> {
    return firstValueFrom(this.http.put<Review>(`/api/reviews/${id}`, request));
  }

  listarPendientes(): Promise<Review[]> {
    return firstValueFrom(this.http.get<Review[]>('/api/reviews/pendientes'));
  }

  listarPorEstado(estado: EstadoReview, pagina: number, tamano: number): Promise<PaginaReviews> {
    return firstValueFrom(
      this.http.get<PaginaReviews>('/api/reviews', {
        params: { estado, pagina, tamano },
      }),
    );
  }

  /** Devuelve una reseña ya moderada a la cola de pendientes. */
  revertir(id: number): Promise<Review> {
    return firstValueFrom(this.http.put<Review>(`/api/reviews/${id}/revertir`, {}));
  }

  moderar(id: number, request: ModerarReviewRequest): Promise<Review> {
    return firstValueFrom(this.http.put<Review>(`/api/reviews/${id}/moderar`, request));
  }
}
