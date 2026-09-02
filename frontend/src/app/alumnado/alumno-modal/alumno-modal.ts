import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { dniValido } from '../../auth/login-page/login-page';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';
import { CampoComponent } from '../../compartido/campo/campo';
import { ModalComponent } from '../../compartido/modal/modal';
import { GradoOpcion } from '../../auth/registro.service';
import { Alumno, EditarAlumnoRequest } from '../alumnado.service';

/** Un año lectivo se escribe con cuatro cifras; el rango evita erratas de tecleo. */
function anioValido(control: AbstractControl<string>): ValidationErrors | null {
  if (!control.value) {
    return null;
  }
  const anio = Number(control.value);
  return Number.isInteger(anio) && anio >= 2000 && anio <= 2100 ? null : { anio: true };
}

/**
 * Ficha editable de un alumno. Cambiar el correo cambia con cuál inicia sesión
 * esa persona, y cambiar el DNI **no** recalcula su contraseña: puede haber
 * entrado ya y haber puesto la suya.
 */
@Component({
  selector: 'app-alumno-modal',
  imports: [ReactiveFormsModule, ModalComponent, CampoComponent, BotonComponent, AlertaComponent],
  templateUrl: './alumno-modal.html',
})
export class AlumnoModalComponent {
  readonly alumno = input.required<Alumno>();
  readonly grados = input.required<GradoOpcion[]>();
  readonly guardando = input(false);
  readonly error = input<string | null>(null);

  readonly guardar = output<EditarAlumnoRequest>();
  readonly cerrar = output<void>();

  protected readonly confirmandoSalida = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', Validators.required],
    apellido1: ['', Validators.required],
    apellido2: [''],
    dni: ['', [Validators.required, dniValido]],
    correo: ['', [Validators.required, Validators.email]],
    gradoId: [''],
    anio: ['', anioValido],
  });

  /** Guarda lo que había al abrir, para saber si hay cambios sin guardar. */
  private readonly inicial = signal('');
  protected readonly sucio = computed(() => this.form.dirty);

  constructor() {
    effect(() => {
      const alumno = this.alumno();
      this.form.setValue({
        nombre: alumno.nombre ?? '',
        apellido1: alumno.apellido1 ?? '',
        apellido2: alumno.apellido2 ?? '',
        dni: alumno.dni ?? '',
        correo: alumno.correo,
        gradoId: alumno.grado ? String(alumno.grado.id) : '',
        anio: alumno.anio ? String(alumno.anio) : '',
      });
      this.form.markAsPristine();
      this.inicial.set(JSON.stringify(this.form.getRawValue()));
    });
  }

  /** Con cambios sin guardar se confirma antes de tirarlos; sin ellos se cierra directo. */
  protected pedirSalida(): void {
    if (JSON.stringify(this.form.getRawValue()) === this.inicial()) {
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
      anio: valores.anio ? Number(valores.anio) : null,
    });
  }
}
