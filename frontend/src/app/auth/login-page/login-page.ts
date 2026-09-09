import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FondoComponent } from '../../compartido/fondo/fondo';
import { IconoComponent } from '../../compartido/icono/icono';
import { CentroService } from '../../centro/centro.service';
import { GradoOpcion, RegistroService } from '../registro.service';
import { LoginFormularioComponent } from '../login-formulario/login-formulario';
import { RegistroFormularioComponent } from '../registro-formulario/registro-formulario';

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
  imports: [IconoComponent, FondoComponent, LoginFormularioComponent, RegistroFormularioComponent],
  templateUrl: './login-page.html',
})
export class LoginPage {
  protected readonly centroService = inject(CentroService);
  private readonly registro = inject(RegistroService);

  protected readonly vista = signal<'login' | 'registro'>('login');

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

  protected abrirRegistro(): void {
    this.vista.set('registro');
    if (this.grados() === null) {
      this.cargarGrados();
    }
  }

  protected abrirLogin(): void {
    this.vista.set('login');
  }

  private async cargarGrados(): Promise<void> {
    try {
      this.grados.set(await this.registro.listarGrados());
    } catch {
      this.errorGrados.set('No se pudo cargar el listado de clases. Inténtalo más tarde.');
    }
  }
}
