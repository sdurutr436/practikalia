import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { Empresa } from '../empresas/empresa.model';
import { Asignacion } from './asignacion.model';
import { AsignacionFormularioPage } from './asignacion-formulario-page/asignacion-formulario-page';
import { AlumnoAsignacionesPage } from './alumno-asignaciones-page/alumno-asignaciones-page';
import { pagina } from '../pruebas';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const EMPRESA_PROFESOR: Empresa = {
  id: 2,
  nombre: 'Beta',
  descripcion: 'Una empresa',
  imagen: null,
  direccion: 'Calle Falsa 123',
  sector: { id: 10, nombre: 'Informática' },
  etiquetas: [],
  publicada: true,
  observaciones: '',
  contactoNombre: '',
  contactoTelefono: '',
  contactoEmail: '',
  creadaPorCorreo: 'profesor@centro.es',
  fechaCreacion: '2026-01-01T00:00:00Z',
};

const ASIGNACION_ABIERTA: Asignacion = {
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
};

describe('sección de asignaciones en el detalle de empresa', () => {
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

  async function loginComo(rol: 'ALUMNO' | 'PROFESOR'): Promise<void> {
    const promesa = auth.login('usuario@centro.es', 'secreta', '');
    http.expectOne('/api/auth/login').flush({ rol, esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  }

  it('profesor ve el histórico de asignaciones y puede cerrar una inline', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA_PROFESOR);
    await esperarMicrotareas();
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http.expectOne('/api/empresas/2/asignaciones').flush([ASIGNACION_ABIERTA]);
    http.expectOne('/api/empresas/2/reviews').flush([]);
    http.expectOne('/api/empresas/2/interesados').flush([]);
    http
      .expectOne('/api/auth/me')
      .flush({
        id: 5,
        correo: 'profesor@centro.es',
        rol: 'PROFESOR',
        esAdmin: false,
        debeCambiarContrasena: false,
        etiquetas: [],
      });
    await esperarMicrotareas();
    harness.detectChanges();

    let texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('alumno@centro.es');
    expect(texto).toContain('DAM');
    expect(texto).toContain('abierta');

    const contenedor = harness.routeNativeElement?.querySelector(
      '.c-ficha-empresa__gestion:last-of-type',
    ) as HTMLElement;
    const inputFecha = contenedor.querySelector('input[type="date"]') as HTMLInputElement;
    const select = contenedor.querySelector('select') as HTMLSelectElement;
    const boton = contenedor.querySelector('button.c-boton') as HTMLButtonElement;
    inputFecha.value = '2027-06-30';
    select.value = 'true';
    // El desplegable se entera por el evento, igual que cuando lo usa una persona.
    select.dispatchEvent(new Event('change'));
    boton.click();

    const peticion = http.expectOne('/api/asignaciones/5');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ fechaFin: '2027-06-30', contratadoPosterior: true });
    peticion.flush({ ...ASIGNACION_ABIERTA, fechaFin: '2027-06-30', contratadoPosterior: true });
    await esperarMicrotareas();
    harness.detectChanges();

    texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('2027-06-30');
    expect(texto).toContain('sí');
  });

  it('un 404 al cerrar deja un mensaje de error legible en la fila', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA_PROFESOR);
    await esperarMicrotareas();
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http.expectOne('/api/empresas/2/asignaciones').flush([ASIGNACION_ABIERTA]);
    http.expectOne('/api/empresas/2/reviews').flush([]);
    http.expectOne('/api/empresas/2/interesados').flush([]);
    http
      .expectOne('/api/auth/me')
      .flush({
        id: 5,
        correo: 'profesor@centro.es',
        rol: 'PROFESOR',
        esAdmin: false,
        debeCambiarContrasena: false,
        etiquetas: [],
      });
    await esperarMicrotareas();
    harness.detectChanges();

    const contenedor = harness.routeNativeElement?.querySelector(
      '.c-ficha-empresa__gestion:last-of-type',
    ) as HTMLElement;
    const inputFecha = contenedor.querySelector('input[type="date"]') as HTMLInputElement;
    const boton = contenedor.querySelector('button.c-boton') as HTMLButtonElement;
    inputFecha.value = '2027-06-30';
    boton.click();

    http
      .expectOne('/api/asignaciones/5')
      .flush({ codigo: 'ASIGNACION_NO_ENCONTRADA' }, { status: 404, statusText: 'Not Found' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('La asignación no existe');
  });
});

describe('guard de profesor sobre el formulario de crear asignación', () => {
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

  it('un alumno no puede acceder al formulario de crear asignación', async () => {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2/asignaciones/nueva');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });
});

describe('formulario de crear asignación', () => {
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

  async function loginComoProfesor(): Promise<void> {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  }

  it('carga los selects reales y crea la asignación con empresaId fijado', async () => {
    await loginComoProfesor();
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl(
      '/empresas/2/asignaciones/nueva',
      AsignacionFormularioPage,
    );

    http
      .expectOne('/api/usuarios?rol=ALUMNO')
      .flush([{ id: 10, correo: 'alumno@centro.es', rol: 'ALUMNO' }]);
    http
      .expectOne('/api/usuarios?rol=PROFESOR')
      .flush([{ id: 30, correo: 'profesor@centro.es', rol: 'PROFESOR' }]);
    http.expectOne('/api/grados').flush([{ id: 40, nombre: 'DAM' }]);
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    c.form.patchValue({
      alumnoId: 10,
      tutorCentroId: 30,
      gradoId: 40,
      anio: 2026,
      fechaInicio: '2026-09-01',
    });
    const envio = c.enviar() as Promise<void>;

    const creacion = http.expectOne('/api/asignaciones');
    expect(creacion.request.method).toBe('POST');
    expect(creacion.request.body).toEqual({
      alumnoId: 10,
      empresaId: 2,
      tutorCentroId: 30,
      gradoId: 40,
      anio: 2026,
      fechaInicio: '2026-09-01',
    });
    creacion.flush(ASIGNACION_ABIERTA);
    await envio;

    http.expectOne('/api/empresas/2').flush(EMPRESA_PROFESOR);
    await esperarMicrotareas();
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http.expectOne('/api/empresas/2/asignaciones').flush([ASIGNACION_ABIERTA]);
    http.expectOne('/api/empresas/2/reviews').flush([]);
    http.expectOne('/api/empresas/2/interesados').flush([]);
    http
      .expectOne('/api/auth/me')
      .flush({
        id: 5,
        correo: 'profesor@centro.es',
        rol: 'PROFESOR',
        esAdmin: false,
        debeCambiarContrasena: false,
        etiquetas: [],
      });
    await esperarMicrotareas();

    expect(router.url).toBe('/empresas/2');
  });

  it('un 409 al repetir (alumno, empresa, grado, año) se muestra legible', async () => {
    await loginComoProfesor();
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl(
      '/empresas/2/asignaciones/nueva',
      AsignacionFormularioPage,
    );

    http
      .expectOne('/api/usuarios?rol=ALUMNO')
      .flush([{ id: 10, correo: 'alumno@centro.es', rol: 'ALUMNO' }]);
    http
      .expectOne('/api/usuarios?rol=PROFESOR')
      .flush([{ id: 30, correo: 'profesor@centro.es', rol: 'PROFESOR' }]);
    http.expectOne('/api/grados').flush([{ id: 40, nombre: 'DAM' }]);
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    c.form.patchValue({
      alumnoId: 10,
      tutorCentroId: 30,
      gradoId: 40,
      anio: 2026,
      fechaInicio: '2026-09-01',
    });
    const envio = c.enviar() as Promise<void>;

    http
      .expectOne('/api/asignaciones')
      .flush({ codigo: 'ASIGNACION_YA_EXISTE' }, { status: 409, statusText: 'Conflict' });
    await envio;

    expect(c.error()).toContain('Ya existe una asignación');
  });
});

describe('guard de profesor sobre el histórico por alumno', () => {
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

  it('un alumno no puede acceder al histórico de asignaciones de otro alumno', async () => {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/alumnos/10/asignaciones');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });
});

describe('histórico de asignaciones por alumno', () => {
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

  it('un profesor ve el histórico completo de un alumno', async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/alumnos/10/asignaciones');
    http.expectOne('/api/alumnos/10/asignaciones').flush([ASIGNACION_ABIERTA]);
    http.expectOne('/api/grados').flush([{ id: 40, nombre: 'DAM' }]);
    await esperarMicrotareas();
    http.expectOne('/api/empresas/2/reviews').flush([]);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Beta');
    expect(texto).toContain('DAM');
    expect(texto).toContain('Escribir review en nombre del alumno');
    expect(
      harness.routeNativeElement?.querySelector('a[href="/alumnos/10/afinidad"]'),
    ).toBeTruthy();
  });

  it('un profesor puede fijar el grado y año de un alumno y ver la confirmación', async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl(
      '/alumnos/10/asignaciones',
      AlumnoAsignacionesPage,
    );
    http.expectOne('/api/alumnos/10/asignaciones').flush([ASIGNACION_ABIERTA]);
    http.expectOne('/api/grados').flush([{ id: 40, nombre: 'DAM' }]);
    await esperarMicrotareas();
    http.expectOne('/api/empresas/2/reviews').flush([]);
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    c.formGrado.patchValue({ gradoId: 40, anio: 2027 });
    const guardado = c.guardarGrado() as Promise<void>;

    const peticion = http.expectOne('/api/usuarios/10/grado');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ gradoId: 40, anio: 2027 });
    peticion.flush({
      id: 10,
      correo: 'alumno@centro.es',
      grado: { id: 40, nombre: 'DAM' },
      anio: 2027,
    });
    await guardado;

    expect(c.gradoActualizado()?.anio).toBe(2027);
  });

  it('un 404 al fijar el grado de un alumno inexistente se muestra legible', async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl(
      '/alumnos/999/asignaciones',
      AlumnoAsignacionesPage,
    );
    http.expectOne('/api/alumnos/999/asignaciones').flush([]);
    http.expectOne('/api/grados').flush([{ id: 40, nombre: 'DAM' }]);
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    c.formGrado.patchValue({ gradoId: 40, anio: 2027 });
    const guardado = c.guardarGrado() as Promise<void>;

    http
      .expectOne('/api/usuarios/999/grado')
      .flush({ codigo: 'USUARIO_NO_ENCONTRADO' }, { status: 404, statusText: 'Not Found' });
    await guardado;

    expect(c.errorGrado()).toContain('El alumno no existe');
  });
});
