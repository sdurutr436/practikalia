import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { IconoComponent } from '../../compartido/icono/icono';
import { AuthService } from '../auth.service';
import { MENSAJES_LOGIN, mensajeDeError } from '../mensajes-error';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, IconoComponent],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);

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
