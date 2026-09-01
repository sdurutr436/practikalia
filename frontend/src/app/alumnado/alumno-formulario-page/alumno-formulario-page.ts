import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MENSAJES_ALTA_ALUMNO, mensajeDeError } from '../../auth/mensajes-error';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';
import { CampoComponent } from '../../compartido/campo/campo';
import { VolverComponent } from '../../compartido/volver/volver';
import { AlumnoCreado, AlumnadoService } from '../alumnado.service';

/**
 * Alta de una cuenta de alumno desde la cabecera, el equivalente de "Nueva
 * empresa" para el alumnado. El centro solo aporta el correo: la contraseña la
 * genera el backend y se enseña una única vez, así que el alta se queda en
 * pantalla hasta que quien la crea la anote.
 */
@Component({
  selector: 'app-alumno-formulario-page',
  imports: [ReactiveFormsModule, VolverComponent, AlertaComponent, CampoComponent, BotonComponent],
  templateUrl: './alumno-formulario-page.html',
})
export class AlumnoFormularioPage {
  private readonly alumnadoService = inject(AlumnadoService);

  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly creado = signal<AlumnoCreado | null>(null);

  protected readonly form = inject(NonNullableFormBuilder).group({
    correo: ['', [Validators.required, Validators.email]],
  });

  protected async enviar(): Promise<void> {
    if (this.guardando()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set(null);
    try {
      this.creado.set(await this.alumnadoService.crear(this.form.getRawValue().correo));
      this.form.reset();
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_ALTA_ALUMNO));
    } finally {
      this.guardando.set(false);
    }
  }
}
