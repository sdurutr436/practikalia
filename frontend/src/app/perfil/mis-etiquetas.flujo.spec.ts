import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { MisEtiquetasPage } from './mis-etiquetas-page/mis-etiquetas-page';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

describe('página "Mis etiquetas"', () => {
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
    await harness.navigateByUrl('/mis-etiquetas');
    // alumnoGuard deniega y "/" redirige al listado.
    http.expectOne('/api/empresas').flush([]);
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('un alumno ve el catálogo completo con sus etiquetas actuales premarcadas', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mis-etiquetas');
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
    http.expectOne('/api/etiquetas').flush([
      { id: 1, nombre: 'Java' },
      { id: 2, nombre: 'Frontend' },
    ]);
    http.expectOne('/api/usuarios/10/etiquetas').flush([{ id: 2, nombre: 'Frontend' }]);
    await esperarMicrotareas();
    harness.detectChanges();

    const checkboxes = [
      ...(harness.routeNativeElement?.querySelectorAll<HTMLInputElement>('input[type="checkbox"]') ?? []),
    ];
    expect(checkboxes.map((c) => c.checked)).toEqual([false, true]);
  });

  it('guardar reemplaza la lista completa y muestra confirmación', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl('/mis-etiquetas', MisEtiquetasPage);
    http.expectOne('/api/auth/me').flush({
      id: 10,
      correo: 'alumno@centro.es',
      rol: 'ALUMNO',
      esAdmin: false,
      debeCambiarContrasena: false,
      etiquetas: [],
    });
    await esperarMicrotareas();
    http.expectOne('/api/etiquetas').flush([
      { id: 1, nombre: 'Java' },
      { id: 2, nombre: 'Frontend' },
    ]);
    http.expectOne('/api/usuarios/10/etiquetas').flush([]);
    await esperarMicrotareas();
    harness.detectChanges();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    c.toggle(1, true);
    const guardado = c.guardar() as Promise<void>;

    const guardar = http.expectOne('/api/usuarios/10/etiquetas');
    expect(guardar.request.method).toBe('PUT');
    expect(guardar.request.body).toEqual({ etiquetaIds: [1] });
    guardar.flush([{ id: 1, nombre: 'Java' }]);
    await guardado;
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Etiquetas guardadas.');
  });

  it('el listado de empresas enlaza a "Mis etiquetas" para alumno, no para profesor', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    http.expectOne('/api/empresas').flush([]);
    await esperarMicrotareas();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Mis etiquetas');
  });
});
