import { Component, inject, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { MENSAJES_LOGIN, mensajeDeError } from '../mensajes-error';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { CampoComponent } from '../../compartido/campo/campo';
import { BotonComponent } from '../../compartido/boton/boton';
import { IconoComponent } from '../../compartido/icono/icono';

/**
 * Formulario de login del acceso. Separado de RegistroFormularioComponent
 * porque no comparten nada más que la tarjeta `.c-formulario` que ya pone
 * el CSS — cada uno con su propio form, validación y envío.
 */
@Component({
  selector: 'app-login-formulario',
  imports: [ReactiveFormsModule, AlertaComponent, CampoComponent, BotonComponent, IconoComponent],
  templateUrl: './login-formulario.html',
})
export class LoginFormularioComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly verContrasena = signal(false);
  protected readonly error = signal<string | null>(null);

  /** El pie "¿No tienes cuenta?" pide cambiar a la vista de registro. */
  readonly crearCuenta = output<void>();

  protected readonly form = inject(NonNullableFormBuilder).group({
    correo: ['', [Validators.required, Validators.email]],
    contrasena: ['', Validators.required],
    // Honeypot: oculto en la plantilla, el backend exige que llegue vacío.
    web: [''],
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
    const { correo, contrasena, web } = this.form.getRawValue();
    try {
      const sesion = await this.auth.login(correo, contrasena, web);
      await this.router.navigate([sesion.debeCambiarContrasena ? '/cambiar-contrasena' : '/']);
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_LOGIN));
    } finally {
      this.enviando.set(false);
    }
  }
}
