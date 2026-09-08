import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Alumno } from '../../alumnado/alumnado.service';
import { AsignacionService } from '../../asignaciones/asignacion.service';
import { AuthService } from '../../auth/auth.service';
import { GradoOpcion, RegistroService } from '../../auth/registro.service';
import { MENSAJES_CUENTA, mensajeDeError } from '../../auth/mensajes-error';
import { BotonComponent } from '../../compartido/boton/boton';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { EstadoComponent } from '../../compartido/estado/estado';
import { IconoComponent } from '../../compartido/icono/icono';
import { nombreCompleto } from '../../compartido/nombre';
import { PaginacionComponent } from '../../compartido/paginacion/paginacion';
import { PastillasComponent } from '../../compartido/pastillas/pastillas';
import { ToastService } from '../../compartido/toast/toast.service';
import { ProfesorModalComponent } from '../profesor-modal/profesor-modal';
import {
  FichaProfesorRequest,
  PaginaProfesores,
  Profesor,
  ProfesoradoService,
} from '../profesorado.service';

/** Tres columnas por tres filas, igual que alumnado y empresas. */
const POR_PAGINA = 9;

/** Todo el alumnado con empresa cabe de sobra: es el reparto de un curso. */
const ALUMNOS_MAXIMOS = 200;

/** Las pastillas. La clave viaja en `?estado=`; lo que filtra es la tutoría de clase. */
const PASTILLAS = [
  { clave: 'todos', conClase: null, etiqueta: 'Todos' },
  { clave: 'con-clase', conClase: true, etiqueta: 'Con clase asignada' },
  { clave: 'sin-clase', conClase: false, etiqueta: 'Sin clase' },
] as const;

const VACIOS: Record<string, string> = {
  todos: 'Todavía no hay profesorado dado de alta.',
  'con-clase': 'Ningún profesor tutoriza una clase todavía.',
  'sin-clase': 'Todo el profesorado tutoriza ya una clase.',
};

/** Mensajes por código del contrato de /api/profesores. */
const MENSAJES_PROFESORADO: Record<string, string> = {
  ...MENSAJES_CUENTA,
  USUARIO_NO_ENCONTRADO: 'Ese profesor ya no existe.',
  ULTIMO_ADMINISTRADOR:
    'Es el único administrador del centro: nombra a otro antes de quitarle el permiso.',
  ASIGNACION_NO_ENCONTRADA: 'Alguno de los alumnos elegidos ya no tiene empresa asignada.',
  ACCESO_DENEGADO: 'Solo un administrador puede editar al profesorado.',
};

/**
 * Listado de profesorado del centro. Mismo esqueleto que alumnado: el estado
 * vive en la URL (`?estado=`, `?pagina=`) y la tarjeta es la misma `.c-persona`.
 * Lo ve cualquier profesor, pero el lápiz y el alta son solo del admin: el
 * profesorado no se edita entre sí.
 */
@Component({
  selector: 'app-profesorado-page',
  imports: [
    EstadoComponent,
    CabeceraComponent,
    BotonComponent,
    IconoComponent,
    PaginacionComponent,
    PastillasComponent,
    ProfesorModalComponent,
  ],
  templateUrl: './profesorado-page.html',
})
export class ProfesoradoPage {
  private readonly profesoradoService = inject(ProfesoradoService);
  private readonly asignacionService = inject(AsignacionService);
  private readonly registroService = inject(RegistroService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly pastillas = PASTILLAS;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly resultado = signal<PaginaProfesores | null>(null);
  protected readonly grados = signal<GradoOpcion[]>([]);
  protected readonly alumnos = signal<Alumno[]>([]);
  protected readonly editando = signal<Profesor | null>(null);
  /** El modal está abierto: con `editando()` a null es un alta. */
  protected readonly fichaAbierta = signal(false);
  protected readonly guardando = signal(false);
  protected readonly errorFicha = signal<string | null>(null);

  /** Dar de alta y editar es exclusivo del admin. */
  protected readonly esAdmin = computed(() => this.auth.sesion()?.esAdmin === true);

  private readonly parametros = toSignal(this.route.queryParamMap, { requireSync: true });

  protected readonly clave = computed(() => {
    const valor = this.parametros().get('estado');
    return PASTILLAS.some((pastilla) => pastilla.clave === valor) ? valor! : 'todos';
  });
  private readonly conClase = computed(
    () => PASTILLAS.find((pastilla) => pastilla.clave === this.clave())!.conClase,
  );
  protected readonly paginaActual = computed(() => Number(this.parametros().get('pagina') ?? 0));

  protected readonly profesores = computed(() => this.resultado()?.contenido ?? []);
  protected readonly paginas = computed(() => this.resultado()?.paginas ?? 0);
  protected readonly total = computed(() => this.resultado()?.total ?? 0);
  protected readonly mensajeVacio = computed(() => VACIOS[this.clave()]);

  constructor() {
    effect(() => {
      this.parametros();
      untracked(() => void this.cargar());
    });
  }

  protected readonly nombreCompleto = nombreCompleto;

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pagina: pagina || null },
      queryParamsHandling: 'merge',
    });
  }

  /**
   * Los dos catálogos de la ficha se piden al abrirla por primera vez y no al
   * entrar en la pantalla: solo los necesita el admin, que es el único que abre
   * el modal.
   */
  protected abrirFicha(profesor: Profesor | null): void {
    this.errorFicha.set(null);
    this.editando.set(profesor);
    this.fichaAbierta.set(true);
    void this.cargarCatalogos();
  }

  protected cerrarFicha(): void {
    this.fichaAbierta.set(false);
    this.editando.set(null);
    this.errorFicha.set(null);
  }

  protected async guardarFicha(ficha: FichaProfesorRequest): Promise<void> {
    if (this.guardando()) {
      return;
    }
    const profesor = this.editando();
    this.guardando.set(true);
    this.errorFicha.set(null);
    try {
      if (profesor) {
        await this.profesoradoService.editar(profesor.id, ficha);
      } else {
        await this.profesoradoService.crear(ficha);
        this.toast.mostrar(
          `${ficha.nombre} ${ficha.apellido1} ya puede entrar con su DNI sin la letra como contraseña.`,
        );
      }
      this.cerrarFicha();
      // La tutoría de clase y la de prácticas se han podido mover de sitio.
      this.alumnos.set([]);
      await this.cargar();
    } catch (e) {
      this.errorFicha.set(mensajeDeError(e, MENSAJES_PROFESORADO));
    } finally {
      this.guardando.set(false);
    }
  }

  private async cargarCatalogos(): Promise<void> {
    // Los dos a la vez: encadenarlos deja el desplegable de alumnado vacío
    // durante un viaje de red de más, y la ficha ya está abierta.
    await Promise.all([this.cargarGrados(), this.cargarAlumnado()]);
  }

  private async cargarGrados(): Promise<void> {
    if (this.grados().length > 0) {
      return;
    }
    try {
      this.grados.set(await this.registroService.listarGrados());
    } catch {
      // Sin catálogo la ficha deja «Sin clase» como única opción; el resto sirve igual.
    }
  }

  private async cargarAlumnado(): Promise<void> {
    if (this.alumnos().length > 0) {
      return;
    }
    try {
      const pagina = await this.asignacionService.listarAlumnadoDelCurso({
        asignado: true,
        pagina: 0,
        tamano: ALUMNOS_MAXIMOS,
      });
      this.alumnos.set(pagina.contenido);
    } catch {
      // Sin alumnado no se puede repartir la tutoría de prácticas, pero la ficha sí se edita.
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    try {
      this.resultado.set(
        await this.profesoradoService.listar(this.conClase(), this.paginaActual(), POR_PAGINA),
      );
      this.error.set(null);
    } catch {
      this.error.set('No se pudo cargar el profesorado.');
    } finally {
      this.cargando.set(false);
    }
  }
}
