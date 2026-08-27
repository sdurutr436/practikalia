import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { Empresa } from './empresa.model';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const EMPRESA_PUBLICADA: Empresa = {
  id: 1,
  nombre: 'Acme',
  descripcion: 'Una empresa',
  imagen: null,
  direccion: 'Calle Falsa 123',
  sector: { id: 10, nombre: 'Informática' },
  etiquetas: [],
};

const EMPRESA_NO_PUBLICADA: Empresa = {
  ...EMPRESA_PUBLICADA,
  id: 2,
  nombre: 'Beta',
  publicada: false,
  observaciones: '',
  contactoNombre: '',
  contactoTelefono: '',
  contactoEmail: '',
  creadaPorCorreo: 'profesor@centro.es',
  fechaCreacion: '2026-01-01T00:00:00Z',
};

describe('listado de empresas', () => {
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

  it('sin sesión, /empresas redirige a login', async () => {
    auth.limpiarSesion();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    expect(router.url).toBe('/login');
  });

  it('alumno ve el listado sin acción de crear', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    // El backend real solo manda publicadas a un alumno — sin campo `publicada`.
    http.expectOne('/api/empresas').flush([EMPRESA_PUBLICADA]);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Acme');
    expect(texto).not.toContain('Nueva empresa');
    // La vista alumno no trae `publicada`, así que nunca puede pintar el badge.
    expect(texto).not.toContain('No publicada');
  });

  it('profesor ve el botón de crear y el badge de no publicada', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    http.expectOne('/api/empresas').flush([EMPRESA_PUBLICADA, EMPRESA_NO_PUBLICADA]);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Nueva empresa');
    expect(texto).toContain('No publicada');
  });

  it('cerrar sesión desde el listado limpia la sesión y vuelve a login', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    http.expectOne('/api/empresas').flush([]);
    await esperarMicrotareas();

    const boton = [...(harness.routeNativeElement?.querySelectorAll('button') ?? [])].find((b) =>
      b.textContent?.includes('Cerrar sesión'),
    );
    boton?.dispatchEvent(new Event('click'));
    http.expectOne('/api/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
    await esperarMicrotareas();

    expect(auth.sesion()).toBeNull();
    expect(router.url).toBe('/login');
  });

  it('un fallo de red al listar muestra un mensaje de error legible', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    http
      .expectOne('/api/empresas')
      .flush({ codigo: 'ERROR_INTERNO' }, { status: 500, statusText: 'Internal Server Error' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('No se pudieron cargar las empresas');
  });
});

describe('detalle de empresa', () => {
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

  it('alumno ve los datos comunes, sin editar ni gestión', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/1');
    http.expectOne('/api/empresas/1').flush(EMPRESA_PUBLICADA);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Acme');
    expect(texto).not.toContain('Editar');
    expect(texto).not.toContain('Datos de gestión');
  });

  it('profesor ve editar, gestión y el badge de no publicada', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA_NO_PUBLICADA);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Editar');
    expect(texto).toContain('Datos de gestión');
    expect(texto).toContain('No publicada');
  });

  it('un 404 muestra un mensaje legible, no un error sin manejar', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/999');
    http
      .expectOne('/api/empresas/999')
      .flush({ codigo: 'EMPRESA_NO_ENCONTRADA' }, { status: 404, statusText: 'Not Found' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('no existe');
  });
});
