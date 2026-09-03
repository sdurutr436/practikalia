import { Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MENSAJES_ASIGNACION, mensajeDeError } from '../../auth/mensajes-error';
import { AsignacionService } from '../asignacion.service';
import { Grado, UsuarioResumen } from '../asignacion.model';
import { VolverComponent } from '../../compartido/volver/volver';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { CampoComponent } from '../../compartido/campo/campo';
import { BotonComponent } from '../../compartido/boton/boton';
import { DesplegableComponent } from '../../compartido/desplegable/desplegable';

@Component({
  selector: 'app-asignacion-formulario-page',
  imports: [
    ReactiveFormsModule,
    VolverComponent,
    AlertaComponent,
    CampoComponent,
    BotonComponent,
    DesplegableComponent,
  ],
  templateUrl: './asignacion-formulario-page.html',
})
export class AsignacionFormularioPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly asignacionService = inject(AsignacionService);

  protected readonly empresaId = Number(this.route.snapshot.paramMap.get('empresaId'));
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly alumnos = signal<UsuarioResumen[]>([]);
  protected readonly tutores = signal<UsuarioResumen[]>([]);
  protected readonly grados = signal<Grado[]>([]);

  // El 0 deshabilitado de cada uno es el «elige uno», no una opción real.
  protected readonly opcionesAlumno = computed(() => [
    { valor: 0, etiqueta: 'Selecciona un alumno', deshabilitada: true },
    ...this.alumnos().map((alumno) => ({ valor: alumno.id, etiqueta: alumno.correo })),
  ]);
  protected readonly opcionesTutor = computed(() => [
    { valor: 0, etiqueta: 'Selecciona un tutor', deshabilitada: true },
    ...this.tutores().map((tutor) => ({ valor: tutor.id, etiqueta: tutor.correo })),
  ]);
  protected readonly opcionesGrado = computed(() => [
    { valor: 0, etiqueta: 'Selecciona un grado', deshabilitada: true },
    ...this.grados().map((grado) => ({ valor: grado.id, etiqueta: grado.nombre })),
  ]);

  protected readonly form = inject(NonNullableFormBuilder).group({
    alumnoId: [0, [Validators.required, Validators.min(1)]],
    tutorCentroId: [0, [Validators.required, Validators.min(1)]],
    gradoId: [0, [Validators.required, Validators.min(1)]],
    anio: [new Date().getFullYear(), [Validators.required, Validators.min(2000)]],
    fechaInicio: ['', Validators.required],
  });

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      const [alumnos, tutores, grados] = await Promise.all([
        this.asignacionService.listarUsuarios('ALUMNO'),
        this.asignacionService.listarUsuarios('PROFESOR'),
        this.asignacionService.listarGrados(),
      ]);
      this.alumnos.set(alumnos);
      this.tutores.set(tutores);
      this.grados.set(grados);
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
      await this.asignacionService.crear({
        alumnoId: valores.alumnoId,
        empresaId: this.empresaId,
        tutorCentroId: valores.tutorCentroId,
        gradoId: valores.gradoId,
        anio: valores.anio,
        fechaInicio: valores.fechaInicio,
      });
      await this.router.navigate(['/empresas', this.empresaId]);
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_ASIGNACION));
    } finally {
      this.guardando.set(false);
    }
  }
}
