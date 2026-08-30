import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconoComponent } from '../../compartido/icono/icono';
import { MENSAJES_REVIEW, mensajeDeError } from '../../auth/mensajes-error';
import { ReviewService } from '../review.service';
import { Review } from '../review.model';

@Component({
  selector: 'app-reviews-pendientes-page',
  imports: [RouterLink, IconoComponent],
  templateUrl: './reviews-pendientes-page.html',
})
export class ReviewsPendientesPage {
  private readonly reviewService = inject(ReviewService);

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly reviews = signal<Review[]>([]);
  protected readonly guardandoId = signal<number | null>(null);
  protected readonly errorModeracion = signal<{ id: number; mensaje: string } | null>(null);

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      this.reviews.set(await this.reviewService.listarPendientes());
    } catch {
      this.error.set('No se pudieron cargar las reviews pendientes.');
    } finally {
      this.cargando.set(false);
    }
  }

  protected async aprobar(review: Review): Promise<void> {
    await this.moderar(review, 'APROBADA', null);
  }

  protected async rechazar(review: Review, motivoRechazo: string): Promise<void> {
    if (!motivoRechazo.trim()) {
      this.errorModeracion.set({ id: review.id, mensaje: 'Indica un motivo de rechazo.' });
      return;
    }
    await this.moderar(review, 'RECHAZADA', motivoRechazo);
  }

  private async moderar(
    review: Review,
    estado: 'APROBADA' | 'RECHAZADA',
    motivoRechazo: string | null,
  ): Promise<void> {
    if (this.guardandoId() !== null) {
      return;
    }
    this.guardandoId.set(review.id);
    this.errorModeracion.set(null);
    try {
      await this.reviewService.moderar(review.id, { estado, motivoRechazo });
      this.reviews.update((lista) => lista.filter((r) => r.id !== review.id));
    } catch (e) {
      this.errorModeracion.set({ id: review.id, mensaje: mensajeDeError(e, MENSAJES_REVIEW) });
    } finally {
      this.guardandoId.set(null);
    }
  }
}
