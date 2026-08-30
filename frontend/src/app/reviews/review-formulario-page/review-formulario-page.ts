import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { IconoComponent } from '../../compartido/icono/icono';
import { MENSAJES_REVIEW, mensajeDeError } from '../../auth/mensajes-error';
import { ReviewService } from '../review.service';
import { CalificacionConfig } from '../review.model';

@Component({
  selector: 'app-review-formulario-page',
  imports: [ReactiveFormsModule, RouterLink, IconoComponent],
  templateUrl: './review-formulario-page.html',
})
export class ReviewFormularioPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly reviewService = inject(ReviewService);

  private readonly reviewId = this.route.snapshot.paramMap.get('id');
  protected readonly esEdicion = this.reviewId !== null;
  private readonly asignacionId = Number(this.route.snapshot.queryParamMap.get('asignacionId'));
  protected readonly empresaId = Number(this.route.snapshot.queryParamMap.get('empresaId'));

  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly config = signal<CalificacionConfig>({ min: 1, max: 5 });

  protected readonly form = inject(NonNullableFormBuilder).group({
    contenido: ['', Validators.required],
    calificacion: [0, Validators.required],
  });

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      const config = await this.reviewService.calificacionConfig();
      this.config.set(config);
      this.form.controls.calificacion.setValidators([
        Validators.required,
        Validators.min(config.min),
        Validators.max(config.max),
      ]);

      if (this.esEdicion) {
        const reviews = await this.reviewService.listarPorEmpresa(this.empresaId);
        const review = reviews.find((r) => r.id === Number(this.reviewId));
        if (!review) {
          this.error.set('No se encontró la review a editar.');
          return;
        }
        this.form.setValue({ contenido: review.contenido, calificacion: review.calificacion });
      } else {
        this.form.controls.calificacion.setValue(config.min);
      }
    } catch {
      this.error.set('No se pudo cargar el formulario.');
    } finally {
      this.cargando.set(false);
    }
  }

  protected async enviar(): Promise<void> {
    if (this.guardando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    const valores = this.form.getRawValue();
    try {
      const review = this.esEdicion
        ? await this.reviewService.editar(Number(this.reviewId), valores)
        : await this.reviewService.crear({ asignacionId: this.asignacionId, ...valores });
      await this.router.navigate(['/empresas', review.empresaId]);
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_REVIEW));
    } finally {
      this.guardando.set(false);
    }
  }
}
