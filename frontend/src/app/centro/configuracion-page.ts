import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../auth/auth.service';
import { MENSAJES_CENTRO, mensajeDeError } from '../auth/mensajes-error';
import { AlertaComponent } from '../compartido/alerta/alerta';
import { BotonComponent } from '../compartido/boton/boton';
import { CabeceraComponent } from '../compartido/cabecera/cabecera';
import { CampoComponent } from '../compartido/campo/campo';
import { EstadoComponent } from '../compartido/estado/estado';
import { IconoComponent } from '../compartido/icono/icono';
import { CentroService, CorreoPermitido } from './centro.service';

/**
 * Configuración del centro: nombre, logo y la whitelist de correos permitidos.
 * La ruta la ve cualquiera —está en el menú de alumnado y de profesorado—,
 * pero solo un admin ve el formulario; el resto, un aviso. Es el propio
 * backend quien tiene la última palabra (403 si alguien fuerza la petición).
 */
@Component({
  selector: 'app-configuracion-page',
  imports: [
    ReactiveFormsModule,
    AlertaComponent,
    BotonComponent,
    CabeceraComponent,
    CampoComponent,
    EstadoComponent,
    IconoComponent,
  ],
  templateUrl: './configuracion-page.html',
})
export class ConfiguracionPage {
  private readonly centroService = inject(CentroService);
  private readonly auth = inject(AuthService);

  protected readonly esAdmin = computed(() => this.auth.sesion()?.esAdmin === true);
  protected readonly centro = this.centroService.centro;

  protected readonly form = inject(NonNullableFormBuilder).group({
    nombre: ['', Validators.required],
  });
  protected readonly guardandoNombre = signal(false);
  protected readonly errorNombre = signal<string | null>(null);

  protected readonly subiendoLogo = signal(false);
  protected readonly errorLogo = signal<string | null>(null);

  protected readonly correos = signal<CorreoPermitido[]>([]);
  protected readonly cargandoCorreos = signal(true);
  protected readonly errorCorreos = signal<string | null>(null);
  protected readonly nuevoCorreo = inject(NonNullableFormBuilder).control('', [
    Validators.required,
    Validators.email,
  ]);
  protected readonly anadiendoCorreo = signal(false);
  protected readonly errorAlta = signal<string | null>(null);
  /** Fila con el borrado pedido, a la espera de confirmar. */
  protected readonly confirmandoBorrado = signal<number | null>(null);
  protected readonly borrandoId = signal<number | null>(null);

  constructor() {
    // El formulario arranca con el nombre que ya haya llegado (o el de
    // respaldo); si la petición del centro resuelve después de montar la
    // página, se rellena en cuanto lo haga.
    effect(() => {
      const nombre = this.centro().nombre;
      untracked(() => {
        if (!this.form.dirty) {
          this.form.setValue({ nombre });
        }
      });
    });

    if (this.esAdmin()) {
      void this.cargarCorreos();
    }
  }

  protected async guardarNombre(): Promise<void> {
    if (this.guardandoNombre()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardandoNombre.set(true);
    this.errorNombre.set(null);
    try {
      await this.centroService.actualizar(this.form.getRawValue().nombre.trim());
      this.form.markAsPristine();
    } catch (e) {
      this.errorNombre.set(mensajeDeError(e, MENSAJES_CENTRO));
    } finally {
      this.guardandoNombre.set(false);
    }
  }

  protected async onLogoSeleccionado(evento: Event): Promise<void> {
    const input = evento.target as HTMLInputElement;
    const fichero = input.files?.[0];
    // Se limpia siempre: si no, volver a elegir el mismo fichero no dispara `change`.
    input.value = '';
    if (!fichero) {
      return;
    }
    this.subiendoLogo.set(true);
    this.errorLogo.set(null);
    try {
      await this.centroService.subirLogo(fichero);
    } catch (e) {
      this.errorLogo.set(mensajeDeError(e, MENSAJES_CENTRO));
    } finally {
      this.subiendoLogo.set(false);
    }
  }

  protected async anadirCorreo(): Promise<void> {
    if (this.anadiendoCorreo()) {
      return;
    }
    if (this.nuevoCorreo.invalid) {
      this.nuevoCorreo.markAsTouched();
      return;
    }
    this.anadiendoCorreo.set(true);
    this.errorAlta.set(null);
    try {
      const creado = await this.centroService.crearCorreo(
        this.nuevoCorreo.value.trim().toLowerCase(),
      );
      this.correos.update((actuales) =>
        [...actuales, creado].sort((a, b) => a.correo.localeCompare(b.correo)),
      );
      this.nuevoCorreo.reset('');
    } catch (e) {
      this.errorAlta.set(mensajeDeError(e, MENSAJES_CENTRO));
    } finally {
      this.anadiendoCorreo.set(false);
    }
  }

  protected async borrar(correo: CorreoPermitido): Promise<void> {
    if (this.borrandoId() !== null) {
      return;
    }
    this.borrandoId.set(correo.id);
    this.errorCorreos.set(null);
    try {
      await this.centroService.borrarCorreo(correo.id);
      this.correos.update((actuales) => actuales.filter((c) => c.id !== correo.id));
      this.confirmandoBorrado.set(null);
    } catch (e) {
      this.errorCorreos.set(mensajeDeError(e, MENSAJES_CENTRO));
    } finally {
      this.borrandoId.set(null);
    }
  }

  private async cargarCorreos(): Promise<void> {
    this.cargandoCorreos.set(true);
    try {
      this.correos.set(await this.centroService.listarCorreos());
      this.errorCorreos.set(null);
    } catch {
      this.errorCorreos.set('No se pudo cargar la whitelist.');
    } finally {
      this.cargandoCorreos.set(false);
    }
  }
}
