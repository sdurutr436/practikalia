import { Component, inject, input } from '@angular/core';
import { FormArray, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BotonComponent } from '../../compartido/boton/boton';
import { IconoComponent } from '../../compartido/icono/icono';
import { TutorEmpresa } from '../empresa.model';

export function filaTutor(fb: NonNullableFormBuilder, tutor?: TutorEmpresa) {
  return fb.group({
    id: fb.control<number | null>(tutor?.id ?? null),
    nombre: [tutor?.nombre ?? '', Validators.required],
    cargo: [tutor?.cargo ?? ''],
    telefono: [tutor?.telefono ?? ''],
    correo: [tutor?.correo ?? ''],
  });
}

export type FilaTutor = ReturnType<typeof filaTutor>;

/**
 * Filas de tutores de empresa de un formulario reactivo, siempre al menos
 * una. Compartido entre el alta de empresa y la edición inline de su ficha
 * — el `FormArray` lo sigue creando y leyendo cada pantalla (para poder
 * precargarlo o mandarlo al guardar), aquí solo se pintan sus filas y el
 * añadir/quitar.
 */
@Component({
  selector: 'app-tutores-empresa',
  imports: [ReactiveFormsModule, BotonComponent, IconoComponent],
  templateUrl: './tutores-empresa.html',
})
export class TutoresEmpresaComponent {
  private readonly fb = inject(NonNullableFormBuilder);

  readonly tutores = input.required<FormArray<FilaTutor>>();

  protected anadir(): void {
    this.tutores().push(filaTutor(this.fb));
  }

  /** La última no se puede quitar: toda empresa necesita un tutor. */
  protected quitar(indice: number): void {
    if (this.tutores().length > 1) {
      this.tutores().removeAt(indice);
    }
  }
}
