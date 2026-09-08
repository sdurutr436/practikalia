import { HttpErrorResponse } from '@angular/common/http';
import { PercentPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EstadoComponent } from '../../compartido/estado/estado';
import { MENSAJES_EMPRESA, MENSAJES_INTERES, mensajeDeError } from '../../auth/mensajes-error';
import { AsignacionService } from '../../asignaciones/asignacion.service';
import { Asignacion, TasaContratacion } from '../../asignaciones/asignacion.model';
import { AuthService } from '../../auth/auth.service';
import { ReviewService } from '../../reviews/review.service';
import { CalificacionConfig, Review } from '../../reviews/review.model';
import { ReviewCardComponent } from '../../reviews/review-card/review-card';
import { InteresService } from '../../intereses/interes.service';
import { Interesado } from '../../intereses/interes.model';
import { EmpresaService } from '../empresa.service';
import { Empresa, EmpresaRequest, Etiqueta, esVistaProfesor } from '../empresa.model';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { VolverComponent } from '../../compartido/volver/volver';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { CampoComponent } from '../../compartido/campo/campo';
import { BotonComponent } from '../../compartido/boton/boton';
import { IconoComponent } from '../../compartido/icono/icono';
import { filaTutor, TutoresEmpresaComponent } from '../tutores-empresa/tutores-empresa';

/** Cada bloque de la ficha que se puede editar con su propio lápiz. */
type Seccion = 'foto' | 'info' | 'etiquetas' | 'descripcion' | 'observaciones' | 'tutores';

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

@Component({
  selector: 'app-empresa-detalle-page',
  imports: [
    RouterLink,
    PercentPipe,
    ReactiveFormsModule,
    EstadoComponent,
    CabeceraComponent,
    VolverComponent,
    AlertaComponent,
    CampoComponent,
    BotonComponent,
    IconoComponent,
    ReviewCardComponent,
    TutoresEmpresaComponent,
  ],
  templateUrl: './empresa-detalle-page.html',
})
export class EmpresaDetallePage {
  private readonly route = inject(ActivatedRoute);
  private readonly empresaService = inject(EmpresaService);
  private readonly asignacionService = inject(AsignacionService);
  private readonly reviewService = inject(ReviewService);
  private readonly interesService = inject(InteresService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly esVistaProfesor = esVistaProfesor;
  protected readonly sesion = this.authService.sesion;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly empresa = signal<Empresa | null>(null);

  protected readonly reviews = signal<Review[]>([]);
  protected readonly cargandoReviews = signal(false);
  protected readonly errorReviews = signal<string | null>(null);
  protected readonly asignacionesSinReview = signal<Asignacion[]>([]);
  /** El rango lo fija cada instituto; sin él la tarjeta de review no pinta estrellas. */
  protected readonly calificacion = signal<CalificacionConfig | null>(null);

  protected readonly interesado = signal(false);
  protected readonly guardandoInteres = signal(false);
  protected readonly errorInteres = signal<string | null>(null);

  protected readonly interesados = signal<Interesado[]>([]);
  protected readonly cargandoInteresados = signal(false);
  protected readonly errorInteresados = signal<string | null>(null);

  protected readonly tasaContratacion = signal<TasaContratacion | null>(null);

  // --- Edición inline: lápiz pequeño por sección, lápiz grande abre/guarda todas a la vez.

  protected readonly editandoTodo = signal(false);
  protected readonly seccionesAbiertas = signal<ReadonlySet<Seccion>>(new Set());
  protected readonly guardandoSeccion = signal<Seccion | 'todo' | null>(null);
  protected readonly errorGuardado = signal<string | null>(null);

  protected readonly catalogosCargados = signal(false);
  protected readonly sectores = signal<Etiqueta[]>([]);
  protected readonly etiquetasDisponibles = signal<Etiqueta[]>([]);
  protected readonly etiquetasSeleccionadas = signal<Set<number>>(new Set());

  protected readonly subiendoImagen = signal(false);
  protected readonly errorImagen = signal<string | null>(null);

  /** Siempre al menos una fila: el backend exige un tutor por empresa. */
  protected readonly tutores = this.fb.array([filaTutor(this.fb)]);

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
    publicada: [false],
    tutores: this.tutores,
  });

  protected toggleEtiqueta(id: number, marcada: boolean): void {
    const seleccion = new Set(this.etiquetasSeleccionadas());
    if (marcada) {
      seleccion.add(id);
    } else {
      seleccion.delete(id);
    }
    this.etiquetasSeleccionadas.set(seleccion);
  }

  protected estaEditando(seccion: Seccion): boolean {
    return this.editandoTodo() || this.seccionesAbiertas().has(seccion);
  }

  /**
   * El lápiz pequeño abre su sección; si ya está abierta, pasa a guardar solo
   * esa. La foto es la excepción: se sube sola al elegir el fichero, así que
   * su lápiz solo abre/cierra el selector, nunca dispara el guardado general.
   */
  protected async alternarSeccion(seccion: Seccion): Promise<void> {
    if (this.editandoTodo()) {
      return;
    }
    if (this.seccionesAbiertas().has(seccion)) {
      if (seccion === 'foto') {
        this.cerrarSeccion(seccion);
      } else {
        await this.guardar(seccion);
      }
      return;
    }
    if (seccion === 'etiquetas') {
      await this.asegurarCatalogos();
    }
    this.seccionesAbiertas.update((abiertas) => new Set(abiertas).add(seccion));
  }

  /** El lápiz grande abre todas las secciones a la vez; si ya estaban abiertas, lo guarda todo. */
  protected async alternarTodo(): Promise<void> {
    if (this.editandoTodo()) {
      await this.guardar('todo');
      return;
    }
    await this.asegurarCatalogos();
    this.editandoTodo.set(true);
    this.seccionesAbiertas.set(new Set());
  }

  private cerrarSeccion(seccion: Seccion): void {
    this.seccionesAbiertas.update((abiertas) => {
      const copia = new Set(abiertas);
      copia.delete(seccion);
      return copia;
    });
  }

  private async asegurarCatalogos(): Promise<void> {
    if (this.catalogosCargados()) {
      return;
    }
    try {
      const empresas = (await this.empresaService.listar()).contenido;
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
      this.catalogosCargados.set(true);
    } catch {
      // ponytail: best-effort — si falla, los desplegables de sector/etiquetas
      // quedan vacíos y se reintenta la próxima vez que se abra una sección.
    }
  }

  private precargarFormulario(empresa: Empresa): void {
    this.form.patchValue({
      nombre: empresa.nombre,
      descripcion: empresa.descripcion ?? '',
      direccion: empresa.direccion ?? '',
      sectorId: empresa.sector.id,
      observaciones: empresa.observaciones ?? '',
      contactoNombre: empresa.contactoNombre ?? '',
      contactoTelefono: empresa.contactoTelefono ?? '',
      contactoEmail: empresa.contactoEmail ?? '',
      publicada: empresa.publicada ?? false,
    });
    this.etiquetasSeleccionadas.set(new Set(empresa.etiquetas.map((e) => e.id)));
    this.tutores.clear();
    for (const tutor of empresa.tutores ?? []) {
      this.tutores.push(filaTutor(this.fb, tutor));
    }
    if (this.tutores.length === 0) {
      // Empresas de antes de que hubiera tutores: se rellena al guardarla.
      this.tutores.push(filaTutor(this.fb));
    }
  }

  private async guardar(seccion: Seccion | 'todo'): Promise<void> {
    const empresaActual = this.empresa();
    if (!empresaActual || this.guardandoSeccion() !== null) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardandoSeccion.set(seccion);
    this.errorGuardado.set(null);
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
      publicada: valores.publicada,
    };
    try {
      const actualizada = await this.empresaService.actualizar(empresaActual.id, request);
      this.empresa.set(actualizada);
      if (seccion === 'todo') {
        this.editandoTodo.set(false);
        this.seccionesAbiertas.set(new Set());
      } else {
        this.cerrarSeccion(seccion);
      }
    } catch (e) {
      this.errorGuardado.set(mensajeDeError(e, MENSAJES_EMPRESA));
    } finally {
      this.guardandoSeccion.set(null);
    }
  }

  protected async onArchivoSeleccionado(evento: Event): Promise<void> {
    const input = evento.target as HTMLInputElement;
    const fichero = input.files?.[0];
    const empresaActual = this.empresa();
    if (!fichero || !empresaActual) {
      return;
    }
    this.subiendoImagen.set(true);
    this.errorImagen.set(null);
    try {
      const actualizada = await this.empresaService.subirImagen(empresaActual.id, fichero);
      this.empresa.set(actualizada);
    } catch (e) {
      this.errorImagen.set(mensajeDeError(e, MENSAJES_EMPRESA));
    } finally {
      this.subiendoImagen.set(false);
      input.value = '';
    }
  }

  // --- Carga inicial de la ficha (lectura).

  constructor() {
    void this.cargar();
  }

  private async cargar(): Promise<void> {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    try {
      const empresa = await this.empresaService.obtener(id);
      this.empresa.set(empresa);
      void this.cargarTasaContratacion(id);
      if (esVistaProfesor(empresa)) {
        this.precargarFormulario(empresa);
        void this.cargarInteresados(id);
      }
      const promesaReviews = this.cargarReviews(id);
      const sesion = await this.authService.completarSesionSiHaceFalta();

      if (!esVistaProfesor(empresa) && sesion?.rol === 'ALUMNO' && sesion.id !== null) {
        const alumnoId = sesion.id;
        void promesaReviews.then(() => this.cargarAsignacionesPropiasSinReview(id, alumnoId));
        void this.cargarEstadoInteres(id, alumnoId);
      }
    } catch (e) {
      this.error.set(
        e instanceof HttpErrorResponse && e.status === 404
          ? 'Esta empresa no existe, o no está publicada.'
          : 'No se pudo cargar la empresa.',
      );
    } finally {
      this.cargando.set(false);
    }
  }

  private async cargarReviews(empresaId: number): Promise<void> {
    this.cargandoReviews.set(true);
    void this.cargarCalificacion();
    try {
      this.reviews.set(await this.reviewService.listarPorEmpresa(empresaId));
    } catch {
      this.errorReviews.set('No se pudieron cargar las reviews.');
    } finally {
      this.cargandoReviews.set(false);
    }
  }

  private async cargarCalificacion(): Promise<void> {
    try {
      this.calificacion.set(await this.reviewService.calificacionConfig());
    } catch {
      // ponytail: best-effort — sin el rango, la tarjeta de review simplemente no pinta estrellas.
    }
  }

  /** Cruza las asignaciones propias del alumno en esta empresa contra las reviews ya cargadas. */
  private async cargarAsignacionesPropiasSinReview(
    empresaId: number,
    alumnoId: number,
  ): Promise<void> {
    try {
      const propias = await this.asignacionService.listarPorAlumno(alumnoId);
      const conReview = new Set(this.reviews().map((r) => r.asignacionId));
      this.asignacionesSinReview.set(
        propias.filter((a) => a.empresaId === empresaId && !conReview.has(a.id)),
      );
    } catch {
      // ponytail: best-effort — si falla, simplemente no se ofrece el atajo de "escribir review" aquí.
    }
  }

  private async cargarTasaContratacion(empresaId: number): Promise<void> {
    try {
      this.tasaContratacion.set(await this.asignacionService.tasaContratacion(empresaId));
    } catch {
      // ponytail: best-effort — si falla, simplemente no se muestra el dato.
    }
  }

  private async cargarInteresados(empresaId: number): Promise<void> {
    this.cargandoInteresados.set(true);
    try {
      this.interesados.set(await this.interesService.listarInteresados(empresaId));
    } catch {
      this.errorInteresados.set('No se pudo cargar la lista de interesados.');
    } finally {
      this.cargandoInteresados.set(false);
    }
  }

  private async cargarEstadoInteres(empresaId: number, alumnoId: number): Promise<void> {
    try {
      const intereses = await this.interesService.listarPorAlumno(alumnoId);
      this.interesado.set(intereses.some((i) => i.empresaId === empresaId));
    } catch {
      // ponytail: best-effort — si falla, el botón queda en su estado inicial "no interesado".
    }
  }

  protected async alternarInteres(empresaId: number): Promise<void> {
    if (this.guardandoInteres()) {
      return;
    }
    this.guardandoInteres.set(true);
    this.errorInteres.set(null);
    try {
      if (this.interesado()) {
        await this.interesService.desmarcar(empresaId);
        this.interesado.set(false);
      } else {
        await this.interesService.marcar(empresaId);
        this.interesado.set(true);
      }
    } catch (e) {
      this.errorInteres.set(mensajeDeError(e, MENSAJES_INTERES));
    } finally {
      this.guardandoInteres.set(false);
    }
  }
}
