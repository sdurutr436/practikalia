import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { IconoComponent } from '../../compartido/icono/icono';
import { AuthService } from '../auth.service';
import { MENSAJES_CAMBIO_CONTRASENA, mensajeDeError } from '../mensajes-error';

// Misma política que el backend (UsuarioService.POLITICA_CONTRASENA).
const POLITICA = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,}$/;

export function politicaContrasena(control: AbstractControl<string>): ValidationErrors | null {
  return POLITICA.test(control.value) ? null : { politica: true };
}

@Component({
  selector: 'app-cambiar-contrasena-page',
  imports: [ReactiveFormsModule, IconoComponent],
  templateUrl: './cambiar-contrasena-page.html',
})
export class CambiarContrasenaPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    contrasenaActual: ['', Validators.required],
    contrasenaNueva: ['', [Validators.required, politicaContrasena]],
  });

  protected async enviar(): Promise<void> {
    if (this.enviando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { contrasenaActual, contrasenaNueva } = this.form.getRawValue();
    try {
      await this.auth.cambiarContrasena(contrasenaActual, contrasenaNueva);
      await this.router.navigate(['/']);
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_CAMBIO_CONTRASENA));
    } finally {
      this.enviando.set(false);
    }
  }
}
