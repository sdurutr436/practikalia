import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { AuthService } from './auth/auth.service';
import { authInterceptor } from './auth/auth.interceptor';
import { pagina } from './pruebas';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

describe('flujo de sesión', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  async function loginComo(debeCambiarContrasena: boolean): Promise<void> {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena });
    await promesa;
  }

  it('login → "/" redirige al listado de empresas', async () => {
    const harness = await RouterTestingHarness.create('/login');
    await loginComo(false);

    await harness.navigateByUrl('/');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('sin sesión, "/" redirige a login sin tocar la API de empresas', async () => {
    const harness = await RouterTestingHarness.create();
    auth.limpiarSesion();
    await harness.navigateByUrl('/');
    expect(router.url).toBe('/login');
  });

  it('con cambio de contraseña pendiente, cualquier ruta acaba en /cambiar-contrasena', async () => {
    const harness = await RouterTestingHarness.create('/login');
    await loginComo(true);

    await harness.navigateByUrl('/');
    expect(router.url).toBe('/cambiar-contrasena');

    // Tras completar el cambio se entra con normalidad, al listado de empresas.
    const promesa = auth.cambiarContrasena('secreta', 'Nueva.1234');
    http
      .expectOne('/api/auth/cambiar-contrasena')
      .flush(null, { status: 204, statusText: 'No Content' });
    await promesa;

    await harness.navigateByUrl('/');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('sin sesión, /cambiar-contrasena redirige a login', async () => {
    const harness = await RouterTestingHarness.create();
    auth.limpiarSesion();
    await harness.navigateByUrl('/cambiar-contrasena');
    expect(router.url).toBe('/login');
  });

  it('un 401 de cualquier llamada a la API limpia la sesión y redirige a login', async () => {
    await RouterTestingHarness.create();
    await loginComo(false);

    TestBed.inject(HttpClient)
      .get('/api/empresas')
      .subscribe({ error: () => undefined });
    http
      .expectOne((r) => r.url === '/api/empresas')
      .flush(
        { codigo: 'NO_AUTENTICADO', mensaje: 'No autenticado' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await esperarMicrotareas();
    expect(auth.sesion()).toBeNull();
    expect(router.url).toBe('/login');
  });
});
