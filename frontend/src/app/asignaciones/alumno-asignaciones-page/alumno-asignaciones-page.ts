import { Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EstadoComponent } from '../../compartido/estado/estado';
import { MENSAJES_GRADO, mensajeDeError } from '../../auth/mensajes-error';
import { AsignacionService } from '../asignacion.service';
import { Asignacion, Grado, UsuarioGrado, textoContratacion } from '../asignacion.model';
import { ReviewService } from '../../reviews/review.service';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { CampoComponent } from '../../compartido/campo/campo';
import { BotonComponent } from '../../compartido/boton/boton';
import { DesplegableComponent } from '../../compartido/desplegable/desplegable';

@Component({
  selector: 'app-alumno-asignaciones-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    EstadoComponent,
    CabeceraComponent,
    AlertaComponent,
    CampoComponent,
    BotonComponent,
    DesplegableComponent,
  ],
  templateUrl: './alumno-asignaciones-page.html',
})
export class AlumnoAsignacionesPage {
  private readonly route = inject(ActivatedRoute);
  private readonly asignacionService = inject(AsignacionService);
  private readonly reviewService = inject(ReviewService);

  protected readonly alumnoId = Number(this.route.snapshot.paramMap.get('alumnoId'));
  protected readonly textoContratacion = textoContratacion;

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly asignaciones = signal<Asignacion[]>([]);
  protected readonly asignacionesConReview = signal<Set<number>>(new Set());

  protected readonly grados = signal<Grado[]>([]);
  protected readonly opcionesGrado = computed(() =>
    this.grados().map((grado) => ({ valor: grado.id, etiqueta: grado.nombre })),
  );
  protected readonly guardandoGrado = signal(false);
  protected readonly errorGrado = signal<string | null>(null);
  protected readonly gradoActualizado = signal<UsuarioGrado | null>(null);

  protected readonly formGrado = inject(NonNullableFormBuilder).group({
    gradoId: [0, [Validators.required, Validators.min(1)]],
    anio: [new Date().getFullYear(), [Validators.required, Validators.min(2000)]],
  });

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      const [asignaciones, grados] = await Promise.all([
        this.asignacionService.listarPorAlumno(this.alumnoId),
        this.asignacionService.listarGrados(),
      ]);
      this.asignaciones.set(asignaciones);
      this.grados.set(grados);
      await this.cargarReviewsExistentes(asignaciones);
    } catch {
      this.error.set('No se pudo cargar el histórico del alumno.');
    } finally {
      this.cargando.set(false);
    }
  }

  protected async guardarGrado(): Promise<void> {
    if (this.guardandoGrado()) {
      return;
    }
    if (this.formGrado.invalid) {
      this.formGrado.markAllAsTouched();
      return;
    }
    this.guardandoGrado.set(true);
    this.errorGrado.set(null);
    this.gradoActualizado.set(null);
    const valores = this.formGrado.getRawValue();
    try {
      const resultado = await this.asignacionService.actualizarGrado(this.alumnoId, {
        gradoId: valores.gradoId,
        anio: valores.anio,
      });
      this.gradoActualizado.set(resultado);
    } catch (e) {
      this.errorGrado.set(mensajeDeError(e, MENSAJES_GRADO));
    } finally {
      this.guardandoGrado.set(false);
    }
  }

  private async cargarReviewsExistentes(asignaciones: Asignacion[]): Promise<void> {
    try {
      const empresaIds = [...new Set(asignaciones.map((a) => a.empresaId))];
      const listas = await Promise.all(
        empresaIds.map((id) => this.reviewService.listarPorEmpresa(id)),
      );
      this.asignacionesConReview.set(new Set(listas.flat().map((r) => r.asignacionId)));
    } catch {
      // ponytail: best-effort — si falla, se ofrece el atajo de más y el backend igual protege con 409.
    }
  }
}
