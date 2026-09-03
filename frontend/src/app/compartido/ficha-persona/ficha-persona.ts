import { Component, input, output, signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AlertaComponent } from '../alerta/alerta';
import { BotonComponent } from '../boton/boton';
import { CampoComponent } from '../campo/campo';
import { ModalComponent } from '../modal/modal';

/**
 * Modal de ficha de una persona del centro: nombre y apellidos, DNI y correo,
 * que es lo que comparten el alta de alumnado y la de profesorado, en dos
 * columnas dentro de `.o-ficha__datos`. Lo que cada pantalla tiene de propio va
 * en dos sitios: lo que sigue completando esa misma rejilla (una clase, un
 * curso) se proyecta con `campoExtra`; lo que es un grupo aparte a todo lo
 * ancho (permisos, tutorías) va en el `ng-content` por defecto, detrás.
 *
 * El formulario lo sigue montando cada pantalla y llega entero por `form`: aquí
 * solo se pintan sus cinco controles comunes, se avisa antes de tirar lo escrito
 * y se emite `guardar` cuando es válido.
 *
 * ponytail: los campos proyectados se atan con `[control]`/`[formControl]`, no
 * con `formControlName`. El contenido proyectado se compila en la plantilla de
 * la pantalla, así que no ve el `[formGroup]` que vive aquí dentro.
 */
@Component({
  selector: 'app-ficha-persona',
  imports: [ReactiveFormsModule, ModalComponent, CampoComponent, BotonComponent, AlertaComponent],
  templateUrl: './ficha-persona.html',
})
export class FichaPersonaComponent {
  readonly form = input.required<FormGroup>();
  readonly titulo = input.required<string>();
  /** Prefijo de los `id` de los campos, para que no choquen con los de la pantalla. */
  readonly prefijo = input.required<string>();
  readonly textoGuardar = input.required<string>();
  readonly guardando = input(false);
  readonly error = input<string | null>(null);
  /** Cambios sin guardar que no viven en el formulario (una lista, una selección). */
  readonly extraSucio = input(false);

  readonly guardar = output<void>();
  readonly cerrar = output<void>();

  protected readonly confirmandoSalida = signal(false);

  protected id(campo: string): string {
    return `${this.prefijo()}-${campo}`;
  }

  /** Con cambios sin guardar se confirma antes de tirarlos; sin ellos se cierra directo. */
  protected pedirSalida(): void {
    if (!this.form().dirty && !this.extraSucio()) {
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
    if (this.form().invalid) {
      this.form().markAllAsTouched();
      return;
    }
    this.guardar.emit();
  }
}
