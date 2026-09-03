import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { dniValido } from '../../auth/login-page/login-page';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';
import { CampoComponent } from '../../compartido/campo/campo';
import { DesplegableComponent } from '../../compartido/desplegable/desplegable';
import { ModalComponent } from '../../compartido/modal/modal';
import { nombreCompleto } from '../../compartido/nombre';
import { GradoOpcion } from '../../auth/registro.service';
import { Alumno } from '../../alumnado/alumnado.service';
import { FichaProfesorRequest, Profesor } from '../profesorado.service';

/**
 * Ficha de profesor, la misma para dar de alta y para editar: con `profesor` en
 * `null` el modal se abre vacío y titula «Nuevo profesor».
 *
 * Lleva las dos tutorías. La de clase es un desplegable porque es exclusiva por
 * los dos lados —una clase, un tutor— y elegirla se la quita a quien la tuviese.
 * La de prácticas es una lista que solo crece: quitarle un alumno a un tutor es
 * dárselo a otro, porque una asignación no puede quedarse sin tutor.
 */
@Component({
  selector: 'app-profesor-modal',
  imports: [
    ReactiveFormsModule,
    ModalComponent,
    CampoComponent,
    BotonComponent,
    AlertaComponent,
    DesplegableComponent,
  ],
  templateUrl: './profesor-modal.html',
})
export class ProfesorModalComponent {
  /** `null` = alta. */
  readonly profesor = input<Profesor | null>(null);
  readonly grados = input.required<GradoOpcion[]>();
  /** Alumnado con asignación abierta: los únicos que pueden tener tutor de prácticas. */
  readonly alumnos = input.required<Alumno[]>();
  readonly guardando = input(false);
  readonly error = input<string | null>(null);

  readonly guardar = output<FichaProfesorRequest>();
  readonly cerrar = output<void>();

  protected readonly confirmandoSalida = signal(false);
  protected readonly clases = computed(() => [
    { valor: '', etiqueta: 'Sin clase' },
    ...this.grados().map((grado) => ({ valor: grado.id, etiqueta: grado.nombre })),
  ]);

  protected readonly esAlta = computed(() => this.profesor() === null);
  protected readonly titulo = computed(() =>
    this.esAlta() ? 'Nuevo profesor' : 'Editar profesor',
  );

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', Validators.required],
    apellido1: ['', Validators.required],
    apellido2: [''],
    dni: ['', [Validators.required, dniValido]],
    correo: ['', [Validators.required, Validators.email]],
    gradoId: [''],
    esAdmin: [false],
  });

  /** El desplegable de altas de tutoría: se vacía en cuanto añade a alguien. */
  protected readonly aAnadir = new FormControl('');
  /** Los que se le suman en esta edición; se guardan al darle a guardar. */
  private readonly anadidos = signal<number[]>([]);

  /** Guarda lo que había al abrir, para saber si hay cambios sin guardar. */
  private readonly inicial = signal('');

  constructor() {
    effect(() => {
      const profesor = this.profesor();
      this.form.setValue({
        nombre: profesor?.nombre ?? '',
        apellido1: profesor?.apellido1 ?? '',
        apellido2: profesor?.apellido2 ?? '',
        dni: profesor?.dni ?? '',
        correo: profesor?.correo ?? '',
        gradoId: profesor?.clase ? String(profesor.clase.id) : '',
        esAdmin: profesor?.esAdmin ?? false,
      });
      this.form.markAsPristine();
      this.anadidos.set([]);
      this.inicial.set(JSON.stringify(this.form.getRawValue()));
    });
  }

  /** Los que ya tutoriza más los que se le acaban de dar. */
  protected readonly tutorizados = computed(() => {
    const id = this.profesor()?.id;
    const anadidos = this.anadidos();
    return this.alumnos().filter(
      (alumno) => (id !== undefined && alumno.tutorId === id) || anadidos.includes(alumno.id),
    );
  });

  /** Los demás, con su tutor actual al lado: elegirlos es quitárselo a ese tutor. */
  protected readonly opcionesAlumno = computed(() => {
    const suyos = new Set(this.tutorizados().map((alumno) => alumno.id));
    return this.alumnos()
      .filter((alumno) => !suyos.has(alumno.id))
      .map((alumno) => ({
        valor: alumno.id,
        etiqueta: `${this.nombre(alumno)} · ${alumno.tutorNombre ?? 'sin tutor'}`,
      }));
  });

  protected nombre(alumno: Alumno): string {
    return nombreCompleto(alumno, alumno.correo);
  }

  protected anadir(id: string): void {
    if (id) {
      this.anadidos.update((ids) => [...ids, Number(id)]);
      this.form.markAsDirty();
    }
    this.aAnadir.setValue('');
  }

  /** Con cambios sin guardar se confirma antes de tirarlos; sin ellos se cierra directo. */
  protected pedirSalida(): void {
    if (JSON.stringify(this.form.getRawValue()) === this.inicial() && this.anadidos().length === 0) {
      this.cerrar.emit();
      return;
    }
    this.confirmandoSalida.set(true);
  }

  protected seguirEditando(): void {
    this.confirmandoSalida.set(false);
  }

  protected enviar(): void {
    if (this.guardando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valores = this.form.getRawValue();
    this.guardar.emit({
      nombre: valores.nombre.trim(),
      apellido1: valores.apellido1.trim(),
      apellido2: valores.apellido2.trim() || null,
      dni: valores.dni.trim().toUpperCase(),
      correo: valores.correo.trim().toLowerCase(),
      gradoId: valores.gradoId ? Number(valores.gradoId) : null,
      esAdmin: valores.esAdmin,
      alumnosPractica: this.anadidos(),
    });
  }
}
