import { Component, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RegistroService } from '../registro.service';
import { MENSAJES_REGISTRO, mensajeDeError } from '../mensajes-error';
import { correoInstitucional, dniValido } from '../validadores';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { CampoComponent } from '../../compartido/campo/campo';
import { BotonComponent } from '../../compartido/boton/boton';
import { DesplegableComponent, OpcionDesplegable } from '../../compartido/desplegable/desplegable';

/**
 * Formulario de alta de alumnado desde el acceso. El catálogo de clases lo
 * sigue cargando y cacheando LoginPage (solo hace falta al abrir esta vista,
 * no en cada visita), así que llega por input en vez de pedirse aquí.
 */
@Component({
  selector: 'app-registro-formulario',
  imports: [
    ReactiveFormsModule,
    AlertaComponent,
    CampoComponent,
    BotonComponent,
    DesplegableComponent,
  ],
  templateUrl: './registro-formulario.html',
})
export class RegistroFormularioComponent {
  private readonly registro = inject(RegistroService);

  readonly opcionesGrado = input.required<readonly OpcionDesplegable[]>();
  readonly errorGrados = input<string | null>(null);

  /** El pie "¿Ya tienes cuenta?" y el botón tras enviar piden volver al login. */
  readonly volver = output<void>();

  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly enviado = signal(false);

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', Validators.required],
    apellido1: ['', Validators.required],
    apellido2: [''],
    dni: ['', [Validators.required, dniValido]],
    gradoId: ['', Validators.required],
    correo: ['', [Validators.required, correoInstitucional]],
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
    const { nombre, apellido1, apellido2, dni, gradoId, correo, web } = this.form.getRawValue();
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
      this.enviado.set(true);
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_REGISTRO));
    } finally {
      this.enviando.set(false);
    }
  }
}
