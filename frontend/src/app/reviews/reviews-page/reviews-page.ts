import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MENSAJES_REVIEW, mensajeDeError } from '../../auth/mensajes-error';
import { AlertaComponent } from '../../compartido/alerta/alerta';
import { BotonComponent } from '../../compartido/boton/boton';
import { CabeceraComponent } from '../../compartido/cabecera/cabecera';
import { EstadoComponent } from '../../compartido/estado/estado';
import { EstrellasComponent } from '../../compartido/estrellas/estrellas';
import { PaginacionComponent } from '../../compartido/paginacion/paginacion';
import { PastillasComponent } from '../../compartido/pastillas/pastillas';
import { RechazoModalComponent } from '../rechazo-modal/rechazo-modal';
import { CalificacionConfig, EstadoReview, PaginaReviews, Review } from '../review.model';
import { ReviewService } from '../review.service';

/** Tres columnas por tres filas, igual que el listado de empresas. */
const POR_PAGINA = 9;

/**
 * Ancho por debajo del cual la rejilla va a una sola columna, y con ella el
 * rechazo se escribe dentro de la propia tarjeta en vez de en un modal.
 * Medido sobre la pantalla real: 585px de viewport da una columna y 605px da
 * dos. Va atado a `$ancho-tarjeta-min` de la hoja de tokens: si cambia el
 * ancho mínimo de tarjeta, hay que volver a medir esto.
 */
const UNA_COLUMNA = '(width < 37.5rem)';

/**
 * Las pastillas de la pantalla. La clave es lo que viaja en `?estado=` (se lee
 * mejor en minúscula que el enum) y `valor` lo que entiende el backend.
 */
const PASTILLAS = [
  { clave: 'pendientes', valor: 'PENDIENTE', etiqueta: 'Pendientes' },
  { clave: 'aprobadas', valor: 'APROBADA', etiqueta: 'Aprobadas' },
  { clave: 'rechazadas', valor: 'RECHAZADA', etiqueta: 'Rechazadas' },
] as const;

const VACIOS: Record<string, string> = {
  pendientes: 'No hay reseñas pendientes de moderar.',
  aprobadas: 'Todavía no se ha aprobado ninguna reseña.',
  rechazadas: 'No hay reseñas rechazadas.',
};

/**
 * Cola de moderación de reseñas, con una pastilla por estado. Todo el estado
 * vive en la URL (`?estado=`, `?pagina=`, `?rechazar=`), igual que en el
 * listado de empresas: se comparte por enlace y el botón de atrás funciona.
 */
@Component({
  selector: 'app-reviews-page',
  imports: [
    RouterLink,
    EstadoComponent,
    CabeceraComponent,
    AlertaComponent,
    BotonComponent,
    EstrellasComponent,
    PaginacionComponent,
    PastillasComponent,
    RechazoModalComponent,
  ],
  templateUrl: './reviews-page.html',
})
export class ReviewsPage {
  private readonly reviewService = inject(ReviewService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly pastillas = PASTILLAS;
  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly resultado = signal<PaginaReviews | null>(null);
  protected readonly calificacion = signal<CalificacionConfig | null>(null);
  protected readonly moderandoId = signal<number | null>(null);
  protected readonly errorModeracion = signal<{ id: number; mensaje: string } | null>(null);
  protected readonly errorRechazo = signal<string | null>(null);
  /** Reseña con el modal de rechazo abierto. */
  protected readonly rechazando = signal<Review | null>(null);

  /** Una columna: el modal desaparece y el motivo se escribe en la tarjeta. */
  protected readonly esMovil = signal(false);

  private readonly parametros = toSignal(this.route.queryParamMap, { requireSync: true });

  protected readonly clave = computed(() => {
    const valor = this.parametros().get('estado');
    return PASTILLAS.some((pastilla) => pastilla.clave === valor) ? valor! : 'pendientes';
  });
  private readonly estado = computed(
    () => PASTILLAS.find((pastilla) => pastilla.clave === this.clave())!.valor as EstadoReview,
  );
  protected readonly paginaActual = computed(() => Number(this.parametros().get('pagina') ?? 0));

  protected readonly reviews = computed(() => this.resultado()?.contenido ?? []);
  protected readonly paginas = computed(() => this.resultado()?.paginas ?? 0);
  protected readonly total = computed(() => this.resultado()?.total ?? 0);
  protected readonly esPendiente = computed(() => this.clave() === 'pendientes');
  protected readonly mensajeVacio = computed(() => VACIOS[this.clave()]);

  constructor() {
    this.seguirLaAnchura();
    void this.cargarCalificacion();
    effect(() => {
      this.parametros();
      untracked(() => void this.cargar());
    });
  }

  /** ¿Esta reseña tiene el motivo desplegado dentro de su tarjeta? */
  protected rechazandoAqui(review: Review): boolean {
    return this.esMovil() && this.rechazando()?.id === review.id;
  }

  private seguirLaAnchura(): void {
    // Opcional a propósito: hay entornos sin media queries (jsdom en los tests,
    // y cualquier render fuera del navegador). Sin ellas se queda en la
    // presentación de escritorio, que funciona a cualquier ancho.
    const consulta = window.matchMedia?.(UNA_COLUMNA);
    if (!consulta) {
      return;
    }
    this.esMovil.set(consulta.matches);
    const alCambiar = (evento: MediaQueryListEvent) => this.esMovil.set(evento.matches);
    consulta.addEventListener('change', alCambiar);
    inject(DestroyRef).onDestroy(() => consulta.removeEventListener('change', alCambiar));
  }

  protected irAPagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pagina: pagina || null },
      queryParamsHandling: 'merge',
    });
  }

  protected async aprobar(review: Review): Promise<void> {
    await this.mutar(review.id, () =>
      this.reviewService.moderar(review.id, { estado: 'APROBADA', motivoRechazo: null }),
    );
  }

  /** Deshace una aprobación o un rechazo: la reseña vuelve a la cola. */
  protected async revertir(review: Review): Promise<void> {
    await this.mutar(review.id, () => this.reviewService.revertir(review.id));
  }

  protected abrirRechazo(review: Review): void {
    this.errorRechazo.set(null);
    this.rechazando.set(review);
  }

  protected cerrarRechazo(): void {
    this.rechazando.set(null);
    this.errorRechazo.set(null);
    // El `?rechazar=` del panel ya cumplió: se quita para que recargar no
    // reabra el modal encima de una reseña que quizá ya no esté pendiente.
    if (this.parametros().get('rechazar')) {
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { rechazar: null },
        queryParamsHandling: 'merge',
        replaceUrl: true,
      });
    }
  }

  protected async confirmarRechazo(motivoRechazo: string): Promise<void> {
    const review = this.rechazando();
    if (!review || this.moderandoId() !== null) {
      return;
    }
    this.moderandoId.set(review.id);
    this.errorRechazo.set(null);
    try {
      await this.reviewService.moderar(review.id, { estado: 'RECHAZADA', motivoRechazo });
      this.rechazando.set(null);
      await this.cargar();
    } catch (e) {
      this.errorRechazo.set(mensajeDeError(e, MENSAJES_REVIEW));
    } finally {
      this.moderandoId.set(null);
    }
  }

  private async mutar(id: number, accion: () => Promise<unknown>): Promise<void> {
    if (this.moderandoId() !== null) {
      return;
    }
    this.moderandoId.set(id);
    this.errorModeracion.set(null);
    try {
      await accion();
      await this.cargar();
    } catch (e) {
      this.errorModeracion.set({ id, mensaje: mensajeDeError(e, MENSAJES_REVIEW) });
    } finally {
      this.moderandoId.set(null);
    }
  }

  private async cargarCalificacion(): Promise<void> {
    try {
      this.calificacion.set(await this.reviewService.calificacionConfig());
    } catch {
      // Sin el rango no se pintan estrellas, pero la cola sigue siendo usable.
    }
  }

  private async cargar(): Promise<void> {
    this.cargando.set(true);
    try {
      this.resultado.set(
        await this.reviewService.listarPorEstado(this.estado(), this.paginaActual(), POR_PAGINA),
      );
      this.error.set(null);
      this.abrirSenalada();
    } catch {
      this.error.set('No se pudieron cargar las reseñas.');
    } finally {
      this.cargando.set(false);
    }
  }

  /** Quien llega desde el panel con `?rechazar=` ya decidió rechazar esa reseña. */
  private abrirSenalada(): void {
    const senalada = Number(this.parametros().get('rechazar'));
    if (!senalada || this.rechazando()) {
      return;
    }
    const review = this.reviews().find((candidata) => candidata.id === senalada);
    if (review) {
      this.abrirRechazo(review);
    }
  }
}
