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
  alumnoCorreo: 'alumno@centro.es',
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

  it('un alumno no puede acceder a la cola de pendientes', async () => {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/reviews/pendientes');
    // profesorGuard deniega y "/" redirige al listado.
    http.expectOne('/api/empresas').flush([]);
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('profesor aprueba una review pendiente', async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/reviews/pendientes');
    http.expectOne('/api/reviews/pendientes').flush([REVIEW]);
    await esperarMicrotareas();
    harness.detectChanges();

    const boton = [...(harness.routeNativeElement?.querySelectorAll('button') ?? [])].find((b) =>
      b.textContent?.includes('Aprobar'),
    );
    boton?.click();

    const peticion = http.expectOne('/api/reviews/1/moderar');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ estado: 'APROBADA', motivoRechazo: null });
    peticion.flush({ ...REVIEW, estado: 'APROBADA' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('No hay reviews pendientes');
  });

  it('rechazar sin motivo muestra un error sin llamar al backend', async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/reviews/pendientes');
    http.expectOne('/api/reviews/pendientes').flush([REVIEW]);
    await esperarMicrotareas();
    harness.detectChanges();

    const boton = [...(harness.routeNativeElement?.querySelectorAll('button') ?? [])].find((b) =>
      b.textContent?.includes('Rechazar'),
    );
    boton?.click();
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Indica un motivo de rechazo');
  });

  it('rechazar con motivo manda el PUT con el motivo', async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/reviews/pendientes');
    http.expectOne('/api/reviews/pendientes').flush([REVIEW]);
    await esperarMicrotareas();
    harness.detectChanges();

    const contenedor = harness.routeNativeElement as HTMLElement;
    const input = contenedor.querySelector('input[type="text"]') as HTMLInputElement;
    input.value = 'Poco detallada.';
    const boton = [...contenedor.querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Rechazar'),
    );
    boton?.click();

    const peticion = http.expectOne('/api/reviews/1/moderar');
    expect(peticion.request.body).toEqual({
      estado: 'RECHAZADA',
      motivoRechazo: 'Poco detallada.',
    });
    peticion.flush({ ...REVIEW, estado: 'RECHAZADA', motivoRechazo: 'Poco detallada.' });
    await esperarMicrotareas();
  });
});
