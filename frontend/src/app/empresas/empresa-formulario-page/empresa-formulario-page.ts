import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MENSAJES_EMPRESA, mensajeDeError } from '../../auth/mensajes-error';
import { EmpresaService } from '../empresa.service';
import { Empresa, EmpresaRequest, Etiqueta, TutorEmpresa } from '../empresa.model';
import { VolverComponent } from '../../compartido/volver/volver';
import { EstadoComponent } from '../../compartido/estado/estado';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { CampoComponent } from '../../compartido/campo/campo';
import { BotonComponent } from '../../compartido/boton/boton';
import { IconoComponent } from '../../compartido/icono/icono';

/** IDs sueltos separados por coma → números válidos (>0), sin duplicados. */
function parseIds(texto: string): number[] {
  return texto
    .split(',')
    .map((valor) => Number(valor.trim()))
    .filter((n) => Number.isInteger(n) && n > 0);
}

function porNombre(a: Etiqueta, b: Etiqueta): number {
  return a.nombre.localeCompare(b.nombre);
}

/**
 * Solo da de alta empresas nuevas — editar una ya creada se hace en su ficha,
 * campo a campo con el lápiz (`EmpresaDetallePage`).
 */
@Component({
  selector: 'app-empresa-formulario-page',
  imports: [
    ReactiveFormsModule,
    VolverComponent,
    EstadoComponent,
    AlertaComponent,
    CampoComponent,
    BotonComponent,
    IconoComponent,
  ],
  templateUrl: './empresa-formulario-page.html',
})
export class EmpresaFormularioPage {
  private readonly router = inject(Router);
  private readonly empresaService = inject(EmpresaService);

  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly sectores = signal<Etiqueta[]>([]);
  protected readonly etiquetasDisponibles = signal<Etiqueta[]>([]);
  protected readonly etiquetasSeleccionadas = signal<Set<number>>(new Set());

  private readonly fb = inject(NonNullableFormBuilder);

  /** Los tutores de empresa: siempre al menos una fila, que el backend exige uno. */
  protected readonly tutores = this.fb.array([this.filaTutor()]);

  protected readonly form = this.fb.group({
    nombre: ['', Validators.required],
    descripcion: [''],
    direccion: [''],
    sectorId: [0, [Validators.required, Validators.min(1)]],
    etiquetasManual: [''],
    observaciones: [''],
    contactoNombre: [''],
    contactoTelefono: [''],
    contactoEmail: [''],
    tutores: this.tutores,
  });

  private filaTutor(tutor?: TutorEmpresa) {
    return this.fb.group({
      id: this.fb.control<number | null>(tutor?.id ?? null),
      nombre: [tutor?.nombre ?? '', Validators.required],
      cargo: [tutor?.cargo ?? ''],
      telefono: [tutor?.telefono ?? ''],
      correo: [tutor?.correo ?? ''],
    });
  }

  protected anadirTutor(): void {
    this.tutores.push(this.filaTutor());
  }

  /** La última no se puede quitar: toda empresa necesita un tutor. */
  protected quitarTutor(indice: number): void {
    if (this.tutores.length > 1) {
      this.tutores.removeAt(indice);
    }
  }

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    try {
      this.construirCatalogos((await this.empresaService.listar()).contenido);
    } catch {
      this.error.set('No se pudo cargar el formulario.');
    } finally {
      this.cargando.set(false);
    }
  }

  private construirCatalogos(empresas: Empresa[]): void {
    const sectores = new Map<number, Etiqueta>();
    const etiquetas = new Map<number, Etiqueta>();
    for (const empresa of empresas) {
      sectores.set(empresa.sector.id, empresa.sector);
      for (const etiqueta of empresa.etiquetas) {
        etiquetas.set(etiqueta.id, etiqueta);
      }
    }
    this.sectores.set([...sectores.values()].sort(porNombre));
    this.etiquetasDisponibles.set([...etiquetas.values()].sort(porNombre));
  }

  protected toggleEtiqueta(id: number, marcada: boolean): void {
    const seleccion = new Set(this.etiquetasSeleccionadas());
    if (marcada) {
      seleccion.add(id);
    } else {
      seleccion.delete(id);
    }
    this.etiquetasSeleccionadas.set(seleccion);
  }

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
    const valores = this.form.getRawValue();
    const etiquetaIds = [
      ...new Set([...this.etiquetasSeleccionadas(), ...parseIds(valores.etiquetasManual)]),
    ];
    const request: EmpresaRequest = {
      nombre: valores.nombre,
      descripcion: valores.descripcion,
      direccion: valores.direccion,
      sectorId: valores.sectorId,
      etiquetaIds,
      observaciones: valores.observaciones,
      tutores: valores.tutores.map((tutor) => ({
        id: tutor.id,
        nombre: tutor.nombre.trim(),
        cargo: tutor.cargo.trim() || null,
        telefono: tutor.telefono.trim() || null,
        correo: tutor.correo.trim() || null,
      })),
      contactoNombre: valores.contactoNombre,
      contactoTelefono: valores.contactoTelefono,
      contactoEmail: valores.contactoEmail,
      publicada: false,
    };
    try {
      const creada = await this.empresaService.crear(request);
      await this.router.navigate(['/empresas', creada.id]);
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_EMPRESA));
    } finally {
      this.guardando.set(false);
    }
  }
}
