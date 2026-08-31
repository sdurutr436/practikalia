import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { FondoComponent } from '../../compartido/fondo/fondo';
import { IconoComponent } from '../../compartido/icono/icono';
import { AuthService } from '../auth.service';
import { MENSAJES_LOGIN, mensajeDeError } from '../mensajes-error';

// Catálogo local de frases. Si algún día las sirve la API, esta constante es
// lo único que cambia: el resto ya trabaja contra una señal.
const FRASES = [
  {
    texto: 'El aprendizaje es un tesoro que seguirá a su dueño a todas partes.',
    autor: 'Proverbio chino',
  },
  {
    texto: 'Dime y lo olvido, enséñame y lo recuerdo, involúcrame y lo aprendo.',
    autor: 'Benjamin Franklin',
  },
  {
    texto: 'La práctica sin teoría es ciega; la teoría sin práctica, estéril.',
    autor: 'Adaptado de Kant',
  },
  { texto: 'Nadie sabe de lo que es capaz hasta que lo intenta.', autor: 'Publio Siro' },
  { texto: 'Educar no es llenar un cubo, es encender un fuego.', autor: 'Atribuido a W. B. Yeats' },
  {
    texto: 'El único lugar donde el éxito llega antes que el trabajo es el diccionario.',
    autor: 'Vidal Sassoon',
  },
  { texto: 'Cuenta lo que has aprendido: enseñar es aprender dos veces.', autor: 'Joseph Joubert' },
  {
    texto: 'La suerte es lo que ocurre cuando la preparación se encuentra con la ocasión.',
    autor: 'Séneca',
  },
  { texto: 'Empieza donde estás, usa lo que tienes, haz lo que puedas.', autor: 'Arthur Ashe' },
  { texto: 'El talento se cultiva en la calma; el carácter, en la tormenta.', autor: 'Stendhal' },
];

const ROTACION_MS = 8000;

// Día del año: el carrusel arranca en la frase que toca hoy y desde ahí gira.
function diaDelAno(): number {
  const hoy = new Date();
  return Math.floor((hoy.getTime() - new Date(hoy.getFullYear(), 0, 0).getTime()) / 86_400_000);
}

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, IconoComponent, FondoComponent],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly verContrasena = signal(false);
  protected readonly error = signal<string | null>(null);

  private readonly indice = signal(diaDelAno());
  protected readonly frase = computed(() => FRASES[this.indice() % FRASES.length]);

  constructor() {
    const rotacion = setInterval(() => this.indice.update((i) => i + 1), ROTACION_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(rotacion));
  }

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
