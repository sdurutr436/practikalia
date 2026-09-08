import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { FondoComponent } from '../../compartido/fondo/fondo';
import { IconoComponent } from '../../compartido/icono/icono';
import { AuthService } from '../auth.service';
import { CentroService } from '../../centro/centro.service';
import { MENSAJES_LOGIN, MENSAJES_REGISTRO, mensajeDeError } from '../mensajes-error';
import { GradoOpcion, RegistroService } from '../registro.service';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { CampoComponent } from '../../compartido/campo/campo';
import { BotonComponent } from '../../compartido/boton/boton';
import { DesplegableComponent } from '../../compartido/desplegable/desplegable';

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

// Letra de control del DNI español: resto de la división entre 23 indexa esta
// cadena. Es una comprobación de formato (número + letra cuadran), no una
// verificación real de matriculación — esa la hace el centro al aprobar la
// cuenta pendiente.
const LETRAS_DNI = 'TRWAGMYFPDXBNJZSQVHLCKE';

export function dniValido(control: AbstractControl<string>): ValidationErrors | null {
  const coincide = /^(\d{8})([A-Za-z])$/.exec(control.value.trim());
  if (!coincide) {
    return { dni: true };
  }
  const [, numero, letra] = coincide;
  return LETRAS_DNI[Number(numero) % 23] === letra.toUpperCase() ? null : { dni: true };
}

// ponytail: solo se conoce el dominio general del centro (el mismo que ya usa
// el placeholder del login). Un dominio propio por institución necesita que
// el backend lo exponga primero — ver fase18_autoregistro_alumnos.md.
const CORREO_INSTITUCIONAL = /^[^\s@]+@g\.educaand\.es$/i;

export function correoInstitucional(control: AbstractControl<string>): ValidationErrors | null {
  return CORREO_INSTITUCIONAL.test(control.value) ? null : { correoInstitucional: true };
}

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    IconoComponent,
    FondoComponent,
    AlertaComponent,
    CampoComponent,
    BotonComponent,
    DesplegableComponent,
  ],
  templateUrl: './login-page.html',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  protected readonly centroService = inject(CentroService);
  private readonly registro = inject(RegistroService);
  private readonly router = inject(Router);

  protected readonly vista = signal<'login' | 'registro'>('login');
  protected readonly enviando = signal(false);
  protected readonly verContrasena = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly enviandoRegistro = signal(false);
  protected readonly errorRegistro = signal<string | null>(null);
  protected readonly registroEnviado = signal(false);
  protected readonly grados = signal<GradoOpcion[] | null>(null);
  protected readonly errorGrados = signal<string | null>(null);
  /** El catálogo de clases tal y como lo pide el desplegable. */
  protected readonly opcionesGrado = computed(() =>
    (this.grados() ?? []).map((grado) => ({ valor: grado.id, etiqueta: grado.nombre })),
  );

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

  protected readonly formRegistro = inject(NonNullableFormBuilder).group({
    nombre: ['', Validators.required],
    apellido1: ['', Validators.required],
    apellido2: [''],
    dni: ['', [Validators.required, dniValido]],
    gradoId: ['', Validators.required],
    correo: ['', [Validators.required, correoInstitucional]],
    web: [''],
  });

  protected abrirRegistro(): void {
    this.vista.set('registro');
    if (this.grados() === null) {
      this.cargarGrados();
    }
  }

  protected abrirLogin(): void {
    this.vista.set('login');
    this.registroEnviado.set(false);
  }

  private async cargarGrados(): Promise<void> {
    try {
      this.grados.set(await this.registro.listarGrados());
    } catch {
      this.errorGrados.set('No se pudo cargar el listado de clases. Inténtalo más tarde.');
    }
  }

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

  protected async enviarRegistro(): Promise<void> {
    if (this.enviandoRegistro()) {
      return;
    }
    if (this.formRegistro.invalid) {
      this.formRegistro.markAllAsTouched();
      return;
    }
    this.enviandoRegistro.set(true);
    this.errorRegistro.set(null);
    const { nombre, apellido1, apellido2, dni, gradoId, correo, web } =
      this.formRegistro.getRawValue();
    try {
      await this.registro.registrar({
        nombre,
        apellido1,
        apellido2: apellido2 || null,
        dni: dni.toUpperCase(),
        gradoId: Number(gradoId),
        correo,
        web,
      });
      this.registroEnviado.set(true);
    } catch (e) {
      this.errorRegistro.set(mensajeDeError(e, MENSAJES_REGISTRO));
    } finally {
      this.enviandoRegistro.set(false);
    }
  }
}
