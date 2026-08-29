import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  CalificacionConfig,
  CrearReviewRequest,
  EditarReviewRequest,
  ModerarReviewRequest,
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

  moderar(id: number, request: ModerarReviewRequest): Promise<Review> {
    return firstValueFrom(this.http.put<Review>(`/api/reviews/${id}/moderar`, request));
  }
}
