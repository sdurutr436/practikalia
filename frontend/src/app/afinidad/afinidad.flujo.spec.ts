import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { AfinidadListado } from './afinidad.model';
import { pagina } from '../pruebas';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const LISTADO_CON_ETIQUETAS: AfinidadListado = {
  alumnoConEtiquetas: true,
  empresas: [
    {
      empresa: {
        id: 1,
        nombre: 'Alfa Devs',
        descripcion: null,
        imagen: null,
        direccion: null,
        sector: { id: 4, nombre: 'Java' },
        etiquetas: [{ id: 4, nombre: 'Java' }],
      },
      score: 1.2,
      etiquetasCoincidentes: [{ id: 4, nombre: 'Java' }],
      sectorCoincide: true,
    },
    {
      empresa: {
        id: 2,
        nombre: 'Beta Marketing',
        descripcion: null,
        imagen: null,
        direccion: null,
        sector: { id: 6, nombre: 'Marketing' },
        etiquetas: [{ id: 6, nombre: 'Marketing' }],
      },
      score: 0,
      etiquetasCoincidentes: [],
      sectorCoincide: false,
    },
  ],
};

const LISTADO_SIN_ETIQUETAS: AfinidadListado = {
  alumnoConEtiquetas: false,
  empresas: [
    {
      empresa: LISTADO_CON_ETIQUETAS.empresas[0].empresa,
      score: 0,
      etiquetasCoincidentes: [],
      sectorCoincide: false,
    },
  ],
};

describe('página de afinidad', () => {
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

  it('un profesor no puede acceder a /mi-afinidad, vuelve al listado', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mi-afinidad');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('un alumno no puede acceder a la afinidad de otro alumno por id', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/alumnos/3/afinidad');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('un alumno con etiquetas ve el ranking con score y explicabilidad', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mi-afinidad');
    http.expectOne('/api/empresas/afinidad').flush(LISTADO_CON_ETIQUETAS);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Alfa Devs');
    expect(texto).toContain('sector afín');
    expect(texto).toContain('1.20');
    expect(texto).toContain('Java');
    expect(texto).toContain('Beta Marketing');
    expect(texto).toContain('Sin etiquetas coincidentes');
    expect(texto).not.toContain('Todavía no has marcado');
  });

  it('un alumno sin etiquetas ve el aviso con enlace a Mis etiquetas, no un error', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mi-afinidad');
    http.expectOne('/api/empresas/afinidad').flush(LISTADO_SIN_ETIQUETAS);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Todavía no has marcado ninguna etiqueta');
    expect(texto).not.toContain('c-alerta--error');
    const enlace = harness.routeNativeElement?.querySelector('a[href="/mis-etiquetas"]');
    expect(enlace).toBeTruthy();
  });

  it('un profesor tutor ve la afinidad de un alumno por su id', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/alumnos/3/afinidad');
    http.expectOne('/api/alumnos/3/afinidad').flush(LISTADO_CON_ETIQUETAS);
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Alfa Devs');
  });

  it('un 403 de profesor sin tutoría activa se muestra legible', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/alumnos/3/afinidad');
    http
      .expectOne('/api/alumnos/3/afinidad')
      .flush({ codigo: 'ACCESO_DENEGADO' }, { status: 403, statusText: 'Forbidden' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain(
      'No eres tutor de ninguna asignación activa',
    );
  });
});
