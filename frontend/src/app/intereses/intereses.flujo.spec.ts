import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { Empresa } from '../empresas/empresa.model';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const EMPRESA_ALUMNO: Empresa = {
  id: 2,
  nombre: 'Beta',
  descripcion: 'Una empresa',
  imagen: null,
  direccion: 'Calle Falsa 123',
  sector: { id: 10, nombre: 'Informática' },
  etiquetas: [],
};

const EMPRESA_PROFESOR: Empresa = {
  ...EMPRESA_ALUMNO,
  publicada: true,
  observaciones: '',
  contactoNombre: '',
  contactoTelefono: '',
  contactoEmail: '',
  creadaPorCorreo: 'profesor@centro.es',
  fechaCreacion: '2026-01-01T00:00:00Z',
};

function botonInteres(harness: RouterTestingHarness): HTMLButtonElement | undefined {
  return [...(harness.routeNativeElement?.querySelectorAll('button') ?? [])].find((b) =>
    b.textContent?.includes('interés'),
  ) as HTMLButtonElement | undefined;
}

describe('interés en el detalle de empresa (vista alumno)', () => {
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

  async function abrirFichaComoAlumno(
    harness: RouterTestingHarness,
    intereses: unknown[],
  ): Promise<void> {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA_ALUMNO);
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
    http.expectOne('/api/alumnos/10/asignaciones').flush([]);
    http.expectOne('/api/alumnos/10/intereses').flush(intereses);
    await esperarMicrotareas();
    harness.detectChanges();
  }

  it('sin interés previo, marca y luego desmarca', async () => {
    const harness = await RouterTestingHarness.create();
    await abrirFichaComoAlumno(harness, []);

    expect(botonInteres(harness)?.textContent).toContain('Marcar interés');

    botonInteres(harness)?.dispatchEvent(new Event('click'));
    const marcar = http.expectOne('/api/empresas/2/interes');
    expect(marcar.request.method).toBe('PUT');
    marcar.flush(null);
    await esperarMicrotareas();
    harness.detectChanges();

    expect(botonInteres(harness)?.textContent).toContain('Quitar interés');

    botonInteres(harness)?.dispatchEvent(new Event('click'));
    const desmarcar = http.expectOne('/api/empresas/2/interes');
    expect(desmarcar.request.method).toBe('DELETE');
    desmarcar.flush(null, { status: 204, statusText: 'No Content' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(botonInteres(harness)?.textContent).toContain('Marcar interés');
  });

  it('con interés previo en esta empresa, el botón empieza en "Quitar interés"', async () => {
    const harness = await RouterTestingHarness.create();
    await abrirFichaComoAlumno(harness, [
      {
        empresaId: 2,
        empresaNombre: 'Beta',
        gradoNombre: 'DAM',
        anio: 2026,
        fechaCreacion: '2026-08-01T00:00:00Z',
      },
    ]);

    expect(botonInteres(harness)?.textContent).toContain('Quitar interés');
  });

  it('un alumno sin grado ve el mensaje legible de ALUMNO_SIN_GRADO', async () => {
    const harness = await RouterTestingHarness.create();
    await abrirFichaComoAlumno(harness, []);

    botonInteres(harness)?.dispatchEvent(new Event('click'));
    http
      .expectOne('/api/empresas/2/interes')
      .flush({ codigo: 'ALUMNO_SIN_GRADO' }, { status: 400, statusText: 'Bad Request' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain(
      'Primero necesitas que un profesor te asigne un grado.',
    );
  });
});

describe('interesados en el detalle de empresa (vista profesor)', () => {
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

  it('lista los alumnos interesados con su grado y año', async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA_PROFESOR);
    await esperarMicrotareas();
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http.expectOne('/api/empresas/2/asignaciones').flush([]);
    http.expectOne('/api/empresas/2/reviews').flush([]);
    http
      .expectOne('/api/empresas/2/interesados')
      .flush([
        {
          alumnoId: 10,
          alumnoCorreo: 'alumno@centro.es',
          gradoNombre: 'DAM',
          anio: 2026,
          fechaCreacion: '2026-08-01T00:00:00Z',
        },
      ]);
    http.expectOne('/api/auth/me').flush({
      id: 5,
      correo: 'profesor@centro.es',
      rol: 'PROFESOR',
      esAdmin: false,
      debeCambiarContrasena: false,
      etiquetas: [],
    });
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Interesados');
    expect(texto).toContain('alumno@centro.es');
    expect(texto).toContain('DAM');
  });
});

describe('página "Mis intereses"', () => {
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

  async function loginComo(rol: 'ALUMNO' | 'PROFESOR'): Promise<void> {
    const promesa = auth.login('usuario@centro.es', 'secreta', '');
    http.expectOne('/api/auth/login').flush({ rol, esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  }

  it('un profesor no puede acceder, vuelve al listado', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mis-intereses');
    // alumnoGuard deniega y "/" redirige al listado.
    http.expectOne('/api/empresas').flush([]);
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('un alumno ve su histórico de intereses', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mis-intereses');
    // La sesión post-login no trae id/correo (asimetría documentada) — se completa con /me.
    http.expectOne('/api/auth/me').flush({
      id: 10,
      correo: 'alumno@centro.es',
      rol: 'ALUMNO',
      esAdmin: false,
      debeCambiarContrasena: false,
      etiquetas: [],
    });
    await esperarMicrotareas();
    http
      .expectOne('/api/alumnos/10/intereses')
      .flush([
        {
          empresaId: 2,
          empresaNombre: 'Beta',
          gradoNombre: 'DAM',
          anio: 2026,
          fechaCreacion: '2026-08-01T00:00:00Z',
        },
      ]);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Beta');
    expect(texto).toContain('DAM');
  });
});
