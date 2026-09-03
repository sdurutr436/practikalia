import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { Empresa } from '../empresas/empresa.model';
import { Review } from './review.model';
import { ReviewFormularioPage } from './review-formulario-page/review-formulario-page';
import { pagina } from '../pruebas';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const EMPRESA: Empresa = {
  id: 2,
  nombre: 'Beta',
  descripcion: 'Una empresa',
  imagen: null,
  direccion: 'Calle Falsa 123',
  sector: { id: 10, nombre: 'Informática' },
  etiquetas: [],
};

const REVIEW: Review = {
  id: 1,
  asignacionId: 5,
  empresaId: 2,
  empresaNombre: 'Beta',
  alumnoCorreo: 'alumno@centro.es',
  alumnoNombre: 'Ana Ruiz',
  autorCorreo: 'alumno@centro.es',
  contenido: 'Buena experiencia.',
  calificacion: 4,
  estado: 'PENDIENTE',
  moderadaPorCorreo: null,
  motivoRechazo: null,
  fechaCreacion: '2026-08-28T10:00:00Z',
  fechaModeracion: null,
};

describe('formulario de review', () => {
  let http: HttpTestingController;
  let router: Router;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  async function loginComoAlumno(): Promise<void> {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  }

  it('crea una review nueva usando el rango de calificación real', async () => {
    await loginComoAlumno();
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl(
      '/reviews/nueva?asignacionId=5&empresaId=2',
      ReviewFormularioPage,
    );

    http.expectOne('/api/reviews/calificacion-config').flush({ min: 1, max: 5 });
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    c.form.patchValue({ contenido: 'Buena experiencia.', calificacion: 4 });
    const envio = c.enviar() as Promise<void>;

    const creacion = http.expectOne('/api/reviews');
    expect(creacion.request.method).toBe('POST');
    expect(creacion.request.body).toEqual({
      asignacionId: 5,
      contenido: 'Buena experiencia.',
      calificacion: 4,
    });
    creacion.flush(REVIEW);
    await envio;

    // Navegación a /empresas/2 monta EmpresaDetallePage, que pide sus propios datos.
    http.expectOne('/api/empresas/2').flush(EMPRESA);
    await esperarMicrotareas();
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http.expectOne('/api/empresas/2/reviews').flush([REVIEW]);
    http.expectOne('/api/auth/me').flush({
      id: 10,
      correo: 'alumno@centro.es',
      rol: 'ALUMNO',
      esAdmin: false,
      debeCambiarContrasena: false,
      etiquetas: [],
    });
    await esperarMicrotareas();
    http.expectOne('/api/alumnos/10/asignaciones').flush([]);
    http.expectOne('/api/alumnos/10/intereses').flush([]);
    await esperarMicrotareas();

    expect(router.url).toBe('/empresas/2');
  });

  it('un 409 al repetir la review de una asignación se muestra legible', async () => {
    await loginComoAlumno();
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl(
      '/reviews/nueva?asignacionId=5&empresaId=2',
      ReviewFormularioPage,
    );

    http.expectOne('/api/reviews/calificacion-config').flush({ min: 1, max: 5 });
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    c.form.patchValue({ contenido: 'Buena experiencia.', calificacion: 4 });
    const envio = c.enviar() as Promise<void>;

    http
      .expectOne('/api/reviews')
      .flush({ codigo: 'REVIEW_YA_EXISTE' }, { status: 409, statusText: 'Conflict' });
    await envio;

    expect(c.error()).toContain('Ya existe una review');
  });

  it('carga la review existente y edita con PUT', async () => {
    await loginComoAlumno();
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl(
      '/reviews/1/editar?empresaId=2',
      ReviewFormularioPage,
    );

    http.expectOne('/api/reviews/calificacion-config').flush({ min: 1, max: 5 });
    await esperarMicrotareas();
    http.expectOne('/api/empresas/2/reviews').flush([REVIEW]);
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    expect(c.form.value.contenido).toBe('Buena experiencia.');
    expect(c.form.value.calificacion).toBe(4);

    c.form.patchValue({ contenido: 'Experiencia editada.', calificacion: 5 });
    const envio = c.enviar() as Promise<void>;

    const edicion = http.expectOne('/api/reviews/1');
    expect(edicion.request.method).toBe('PUT');
    expect(edicion.request.body).toEqual({ contenido: 'Experiencia editada.', calificacion: 5 });
    edicion.flush({ ...REVIEW, contenido: 'Experiencia editada.', calificacion: 5 });
    await envio;

    http.expectOne('/api/empresas/2').flush(EMPRESA);
    await esperarMicrotareas();
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http
      .expectOne('/api/empresas/2/reviews')
      .flush([{ ...REVIEW, contenido: 'Experiencia editada.', calificacion: 5 }]);
    http.expectOne('/api/auth/me').flush({
      id: 10,
      correo: 'alumno@centro.es',
      rol: 'ALUMNO',
      esAdmin: false,
      debeCambiarContrasena: false,
      etiquetas: [],
    });
    await esperarMicrotareas();
    http.expectOne('/api/alumnos/10/asignaciones').flush([]);
    http.expectOne('/api/alumnos/10/intereses').flush([]);
    await esperarMicrotareas();

    expect(router.url).toBe('/empresas/2');
  });
});

describe('entrada desde la ficha de empresa (alumno)', () => {
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  it('muestra "Escribir review" para una asignación propia sin review todavía', async () => {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA);
    await esperarMicrotareas();
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http.expectOne('/api/empresas/2/reviews').flush([]);
    http.expectOne('/api/auth/me').flush({
      id: 10,
      correo: 'alumno@centro.es',
      rol: 'ALUMNO',
      esAdmin: false,
      debeCambiarContrasena: false,
      etiquetas: [],
    });
    await esperarMicrotareas();
    http.expectOne('/api/alumnos/10/asignaciones').flush([
      {
        id: 5,
        alumnoId: 10,
        alumnoCorreo: 'alumno@centro.es',
        empresaId: 2,
        empresaNombre: 'Beta',
        tutorCentroId: 30,
        tutorCentroCorreo: 'profesor@centro.es',
        grado: { id: 40, nombre: 'DAM' },
        anio: 2026,
        fechaInicio: '2026-09-01',
        fechaFin: null,
        contratadoPosterior: null,
      },
    ]);
    http.expectOne('/api/alumnos/10/intereses').flush([]);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Escribir review');
  });
});

describe('cola de moderación', () => {
  let http: HttpTestingController;
  let router: Router;
  let auth: AuthService;

  const CONFIG = { min: 1, max: 5 };
  const MOTIVO = 'Falta detalle sobre las tareas que hizo.';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  const entrarComoProfesor = async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  };

  /** La pantalla pide el rango de estrellas y la página a la vez al arrancar. */
  const responderCarga = (reviews: Review[]) => {
    http.expectOne('/api/reviews/calificacion-config').flush(CONFIG);
    http.expectOne((r) => r.url === '/api/reviews').flush(pagina(reviews));
  };

  const abrir = async (url: string, reviews: Review[]) => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    responderCarga(reviews);
    await esperarMicrotareas();
    harness.detectChanges();
    return harness;
  };

  const boton = (raiz: Element, texto: string) =>
    [...raiz.querySelectorAll('button')].find((b) => b.textContent?.includes(texto));

  it('un alumno no puede acceder a la cola', async () => {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/reviews');
    // profesorGuard deniega y "/" redirige al listado.
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('profesor aprueba una reseña pendiente', async () => {
    await entrarComoProfesor();
    const harness = await abrir('/reviews', [REVIEW]);

    boton(harness.routeNativeElement as Element, 'Aprobar')?.click();

    const peticion = http.expectOne('/api/reviews/1/moderar');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ estado: 'APROBADA', motivoRechazo: null });
    peticion.flush({ ...REVIEW, estado: 'APROBADA' });
    await esperarMicrotareas();

    // Tras moderar se recarga la página actual de la cola.
    http.expectOne((r) => r.url === '/api/reviews').flush(pagina([]));
    await esperarMicrotareas();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('No hay reseñas pendientes');
  });

  it('la pastilla de aprobadas pide ese estado y ofrece devolver a pendientes', async () => {
    await entrarComoProfesor();
    const aprobada: Review = { ...REVIEW, estado: 'APROBADA' };

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/reviews?estado=aprobadas');
    http.expectOne('/api/reviews/calificacion-config').flush(CONFIG);
    const carga = http.expectOne((r) => r.url === '/api/reviews');
    expect(carga.request.params.get('estado')).toBe('APROBADA');
    carga.flush(pagina([aprobada]));
    await esperarMicrotareas();
    harness.detectChanges();

    const raiz = harness.routeNativeElement as Element;
    expect(boton(raiz, 'Aprobar')).toBeUndefined();
    boton(raiz, 'Devolver a pendientes')?.click();

    const peticion = http.expectOne('/api/reviews/1/revertir');
    expect(peticion.request.method).toBe('PUT');
    peticion.flush({ ...REVIEW, estado: 'PENDIENTE' });
    await esperarMicrotareas();
    http.expectOne((r) => r.url === '/api/reviews').flush(pagina([]));
    await esperarMicrotareas();
  });

  it('llegar con ?rechazar= abre el modal con esa reseña dentro', async () => {
    await entrarComoProfesor();
    const harness = await abrir('/reviews?rechazar=1', [REVIEW]);

    const dialogo = harness.routeNativeElement?.querySelector('dialog');
    expect(dialogo).toBeTruthy();
    expect(dialogo?.textContent).toContain('Rechazar reseña');
    expect(dialogo?.textContent).toContain(REVIEW.contenido);
    expect(dialogo?.textContent).toContain('Ana Ruiz');
  });

  it('el motivo por debajo del mínimo deja el botón deshabilitado', async () => {
    await entrarComoProfesor();
    const harness = await abrir('/reviews', [REVIEW]);
    const raiz = harness.routeNativeElement as Element;

    boton(raiz, 'Rechazar')?.click();
    harness.detectChanges();

    const area = raiz.querySelector('textarea') as HTMLTextAreaElement;
    area.value = 'Corto';
    area.dispatchEvent(new Event('input'));
    harness.detectChanges();

    expect(boton(raiz, 'Confirmar rechazo')?.disabled).toBe(true);
    expect(raiz.textContent).toContain('Faltan 15 caracteres');
  });

  it('rechazar con un motivo válido manda el PUT con el motivo', async () => {
    await entrarComoProfesor();
    const harness = await abrir('/reviews', [REVIEW]);
    const raiz = harness.routeNativeElement as Element;

    boton(raiz, 'Rechazar')?.click();
    harness.detectChanges();

    const area = raiz.querySelector('textarea') as HTMLTextAreaElement;
    area.value = MOTIVO;
    area.dispatchEvent(new Event('input'));
    harness.detectChanges();

    boton(raiz, 'Confirmar rechazo')?.click();

    const peticion = http.expectOne('/api/reviews/1/moderar');
    expect(peticion.request.body).toEqual({ estado: 'RECHAZADA', motivoRechazo: MOTIVO });
    peticion.flush({ ...REVIEW, estado: 'RECHAZADA', motivoRechazo: MOTIVO });
    await esperarMicrotareas();
    http.expectOne((r) => r.url === '/api/reviews').flush(pagina([]));
    await esperarMicrotareas();
  });

  it('salir con el motivo escrito pide confirmación antes de tirarlo', async () => {
    await entrarComoProfesor();
    const harness = await abrir('/reviews', [REVIEW]);
    const raiz = harness.routeNativeElement as Element;

    boton(raiz, 'Rechazar')?.click();
    harness.detectChanges();

    const area = raiz.querySelector('textarea') as HTMLTextAreaElement;
    area.value = MOTIVO;
    area.dispatchEvent(new Event('input'));
    harness.detectChanges();

    boton(raiz, 'Dejar de escribir')?.click();
    harness.detectChanges();

    // Segundo modal encima del primero: los dos <dialog> siguen abiertos.
    expect(raiz.querySelectorAll('dialog').length).toBe(2);
    expect(raiz.textContent).toContain('¿Seguro que quieres salir?');

    boton(raiz, 'Seguir escribiendo')?.click();
    harness.detectChanges();
    expect(raiz.querySelectorAll('dialog').length).toBe(1);
    expect((raiz.querySelector('textarea') as HTMLTextAreaElement).value).toBe(MOTIVO);

    boton(raiz, 'Dejar de escribir')?.click();
    harness.detectChanges();
    boton(raiz, 'Salir sin guardar')?.click();
    harness.detectChanges();
    expect(raiz.querySelector('dialog')).toBeNull();
  });

  it('con el motivo vacío, dejar de escribir cierra sin preguntar', async () => {
    await entrarComoProfesor();
    const harness = await abrir('/reviews', [REVIEW]);
    const raiz = harness.routeNativeElement as Element;

    boton(raiz, 'Rechazar')?.click();
    harness.detectChanges();
    boton(raiz, 'Dejar de escribir')?.click();
    harness.detectChanges();

    expect(raiz.querySelector('dialog')).toBeNull();
  });

  it('en una columna el motivo se escribe dentro de la tarjeta, sin modal', async () => {
    // jsdom no evalúa media queries de verdad: siempre responde matches:false.
    const original = window.matchMedia;
    window.matchMedia = ((consulta: string) => ({
      matches: true,
      media: consulta,
      addEventListener: () => {},
      removeEventListener: () => {},
    })) as unknown as typeof window.matchMedia;

    try {
      await entrarComoProfesor();
      const harness = await abrir('/reviews', [REVIEW]);
      const raiz = harness.routeNativeElement as Element;

      boton(raiz, 'Rechazar')?.click();
      harness.detectChanges();

      expect(raiz.querySelector('dialog')).toBeNull();
      const area = raiz.querySelector('textarea') as HTMLTextAreaElement;
      expect(raiz.querySelector('.c-rechazo')?.contains(area)).toBe(true);

      area.value = MOTIVO;
      area.dispatchEvent(new Event('input'));
      harness.detectChanges();
      boton(raiz, 'Confirmar rechazo')?.click();

      const peticion = http.expectOne('/api/reviews/1/moderar');
      expect(peticion.request.body).toEqual({ estado: 'RECHAZADA', motivoRechazo: MOTIVO });
      peticion.flush({ ...REVIEW, estado: 'RECHAZADA', motivoRechazo: MOTIVO });
      await esperarMicrotareas();
      http.expectOne((r) => r.url === '/api/reviews').flush(pagina([]));
      await esperarMicrotareas();
    } finally {
      window.matchMedia = original;
    }
  });

  it('en una columna, dejar de escribir cierra sin confirmar aunque haya texto', async () => {
    const original = window.matchMedia;
    window.matchMedia = ((consulta: string) => ({
      matches: true,
      media: consulta,
      addEventListener: () => {},
      removeEventListener: () => {},
    })) as unknown as typeof window.matchMedia;

    try {
      await entrarComoProfesor();
      const harness = await abrir('/reviews', [REVIEW]);
      const raiz = harness.routeNativeElement as Element;

      boton(raiz, 'Rechazar')?.click();
      harness.detectChanges();
      const area = raiz.querySelector('textarea') as HTMLTextAreaElement;
      area.value = MOTIVO;
      area.dispatchEvent(new Event('input'));
      harness.detectChanges();

      boton(raiz, 'Dejar de escribir')?.click();
      harness.detectChanges();

      expect(raiz.querySelector('textarea')).toBeNull();
      expect(raiz.textContent).not.toContain('Seguro que quieres salir');
    } finally {
      window.matchMedia = original;
    }
  });
});
