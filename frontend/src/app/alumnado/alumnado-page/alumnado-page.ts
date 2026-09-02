import {
  Component,
  computed,
  effect,
  inject,
  signal,
  untracked,
  viewChild,
  ElementRef,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../auth/auth.service';
import { GradoOpcion, RegistroService } from '../../auth/registro.service';
import { mensajeDeError } from '../../auth/mensajes-error';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { EstadoComponent } from '../../compartido/estado/estado';
import { IconoComponent } from '../../compartido/icono/icono';
import { AlumnoModalComponent } from '../alumno-modal/alumno-modal';
import { Alumno, AlumnadoService, EditarAlumnoRequest, PaginaAlumnos } from '../alumnado.service';

/** Tres columnas por tres filas, igual que empresas y reseñas. */
const POR_PAGINA = 9;

/**
 * Las pastillas. La clave viaja en `?estado=`; `activo` es lo que entiende el
 * backend, que no tiene un campo "confirmado" — confirmar es poner `activo`.
 */
const PASTILLAS = [
  { clave: 'todos', activo: null, etiqueta: 'Todos' },
  { clave: 'confirmados', activo: true, etiqueta: 'Confirmados' },
  { clave: 'por-confirmar', activo: false, etiqueta: 'Por confirmar' },
] as const;

const VACIOS: Record<string, string> = {
  todos: 'Todavía no hay alumnado. Importa un CSV para darlo de alta en bloque.',
  confirmados: 'Ningún alumno confirmado todavía.',
  'por-confirmar': 'No queda nadie por confirmar.',
};

/**
 * Mensajes por código del contrato de /api/alumnos. `CSV_INVALIDO` no está a
 * propósito: el backend manda en `mensaje` la línea y el motivo concretos, que
 * es lo único con lo que se puede corregir el fichero, y `mensajeDeError` lo
 * usa tal cual cuando el código no está mapeado aquí.
 */
const MENSAJES_ALUMNADO: Record<string, string> = {
  CORREO_YA_EXISTE: 'Ya hay otra cuenta con ese correo.',
  DNI_INVALIDO: 'El DNI no es válido: revisa el número y la letra.',
  USUARIO_NO_ENCONTRADO: 'Ese alumno ya no existe.',
  GRADO_NO_ENCONTRADO: 'La clase seleccionada ya no existe.',
  ACCESO_DENEGADO: 'Solo un administrador puede confirmar cuentas.',
};

/**
 * Listado de alumnado del centro. Mismo esqueleto que empresas y reseñas: el
 * estado vive en la URL (`?estado=`, `?pagina=`) y las tarjetas son pequeñas
 * porque en Practikalia el alumnado no tiene foto de perfil.
 */
@Component({
  selector: 'app-alumnado-page',
  imports: [
    RouterLink,
    EstadoComponent,
    CabeceraComponent,
    AlertaComponent,
    BotonComponent,
    IconoComponent,
    AlumnoModalComponent,
  ],
  templateUrl: './alumnado-page.html',
})
export class AlumnadoPage {
  private readonly alumnadoService = inject(AlumnadoService);
  private readonly registroService = inject(RegistroService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly pastillas = PASTILLAS;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly resultado = signal<PaginaAlumnos | null>(null);
  protected readonly grados = signal<GradoOpcion[]>([]);
  protected readonly editando = signal<Alumno | null>(null);
  protected readonly guardando = signal(false);
  protected readonly errorFicha = signal<string | null>(null);
  protected readonly confirmandoId = signal<number | null>(null);
  protected readonly aviso = signal<string | null>(null);
  protected readonly errorImportacion = signal<string | null>(null);

  private readonly selector = viewChild.required<ElementRef<HTMLInputElement>>('selector');

  /** Confirmar es exclusivo del admin (`PUT /api/usuarios/{id}/activar`). */
  protected readonly esAdmin = computed(() => this.auth.sesion()?.esAdmin === true);

  private readonly parametros = toSignal(this.route.queryParamMap, { requireSync: true });

  protected readonly clave = computed(() => {
    const valor = this.parametros().get('estado');
    return PASTILLAS.some((pastilla) => pastilla.clave === valor) ? valor! : 'todos';
  });
  private readonly activo = computed(
    () => PASTILLAS.find((pastilla) => pastilla.clave === this.clave())!.activo,
  );
  protected readonly paginaActual = computed(() => Number(this.parametros().get('pagina') ?? 0));

  protected readonly alumnos = computed(() => this.resultado()?.contenido ?? []);
  protected readonly paginas = computed(() => this.resultado()?.paginas ?? 0);
  protected readonly total = computed(() => this.resultado()?.total ?? 0);
  protected readonly mensajeVacio = computed(() => VACIOS[this.clave()]);

  constructor() {
    void this.cargarGrados();
    effect(() => {
      this.parametros();
      untracked(() => void this.cargar());
    });
  }

  protected nombreCompleto(alumno: Alumno): string {
    const partes = [alumno.nombre, alumno.apellido1, alumno.apellido2].filter(Boolean);
    return partes.length > 0 ? partes.join(' ') : 'Sin nombre';
  }

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pagina: pagina || null },
      queryParamsHandling: 'merge',
    });
  }

  protected abrirFicha(alumno: Alumno): void {
    this.errorFicha.set(null);
    this.editando.set(alumno);
  }

  protected cerrarFicha(): void {
    this.editando.set(null);
    this.errorFicha.set(null);
  }

  protected async guardarFicha(cambios: EditarAlumnoRequest): Promise<void> {
    const alumno = this.editando();
    if (!alumno || this.guardando()) {
      return;
    }
    this.guardando.set(true);
    this.errorFicha.set(null);
    try {
      await this.alumnadoService.editar(alumno.id, cambios);
      this.editando.set(null);
      await this.cargar();
    } catch (e) {
      this.errorFicha.set(mensajeDeError(e, MENSAJES_ALUMNADO));
    } finally {
      this.guardando.set(false);
    }
  }

  protected async confirmar(alumno: Alumno): Promise<void> {
    if (this.confirmandoId() !== null) {
      return;
    }
    this.confirmandoId.set(alumno.id);
    this.aviso.set(null);
    try {
      await this.alumnadoService.confirmar(alumno.id);
      await this.cargar();
      this.aviso.set(
        `${this.nombreCompleto(alumno)} ya puede entrar. Su contraseña es su DNI sin la letra, y tendrá que cambiarla al primer acceso.`,
      );
    } catch (e) {
      this.error.set(mensajeDeError(e, MENSAJES_ALUMNADO));
    } finally {
      this.confirmandoId.set(null);
    }
  }

  protected async descargarPlantilla(): Promise<void> {
    const csv = await this.alumnadoService.plantillaCsv();
    const url = URL.createObjectURL(csv);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = 'alumnado.csv';
    enlace.click();
    URL.revokeObjectURL(url);
  }

  protected elegirFichero(): void {
    this.selector().nativeElement.click();
  }

  protected async importar(evento: Event): Promise<void> {
    const entrada = evento.target as HTMLInputElement;
    const fichero = entrada.files?.[0];
    // Se limpia siempre: si no, volver a elegir el mismo fichero no dispara `change`.
    entrada.value = '';
    if (!fichero) {
      return;
    }
    this.errorImportacion.set(null);
    this.aviso.set(null);
    try {
      const { creados } = await this.alumnadoService.importar(fichero);
      this.aviso.set(
        `${creados} ${creados === 1 ? 'alumno importado' : 'alumnos importados'}. Quedan por confirmar.`,
      );
      await this.irAPastilla('por-confirmar');
    } catch (e) {
      this.errorImportacion.set(mensajeDeError(e, MENSAJES_ALUMNADO));
    }
  }

  private irAPastilla(clave: string): Promise<boolean> {
    return this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { estado: clave, pagina: null },
      queryParamsHandling: 'merge',
    });
  }

  private async cargarGrados(): Promise<void> {
    try {
      this.grados.set(await this.registroService.listarGrados());
    } catch {
      // Sin catálogo el modal deja "Sin clase" como única opción; el resto sirve igual.
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    try {
      this.resultado.set(
        await this.alumnadoService.listar(this.activo(), this.paginaActual(), POR_PAGINA),
      );
      this.error.set(null);
    } catch {
      this.error.set('No se pudo cargar el alumnado.');
    } finally {
      this.cargando.set(false);
    }
  }
}
