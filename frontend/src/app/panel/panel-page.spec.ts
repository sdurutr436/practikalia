import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PanelPage } from './panel-page';
import { AuthService } from '../auth/auth.service';
import { pagina } from '../pruebas';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const EMPRESA = {
  id: 7,
  nombre: 'Grupo Ondara Software',
  descripcion: null,
  imagen: null,
  direccion: null,
  sector: { id: 1, nombre: 'Desarrollo web' },
  etiquetas: [],
  publicada: true,
};

const PENDIENTE = {
  id: 3,
  asignacionId: 1,
  empresaId: 7,
  alumnoCorreo: 'lucia@centro.es',
  autorCorreo: 'lucia@centro.es',
  contenido: 'Tareas reales desde el primer día.',
  calificacion: 4,
  estado: 'PENDIENTE',
  moderadaPorCorreo: null,
  motivoRechazo: null,
  fechaCreacion: '2026-06-20T10:00:00Z',
  fechaModeracion: null,
};

const RESUMEN = {
  empresasPublicadas: 12,
  empresasSinPublicar: 3,
  alumnadoActivo: 40,
  alumnadoSinAsignar: 9,
};

describe('panel del profesorado', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).sesion.set({
      id: 5,
      correo: 'robles@centro.es',
      rol: 'PROFESOR',
      esAdmin: false,
      debeCambiarContrasena: false,
    });
  });

  afterEach(() => http.verify());

  async function pintar() {
    const fixture = TestBed.createComponent(PanelPage);
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA]));
    await esperarMicrotareas();
    http.expectOne('/api/panel/resumen').flush(RESUMEN);
    await esperarMicrotareas();
    http.expectOne('/api/reviews/pendientes').flush([PENDIENTE]);
    await esperarMicrotareas();
    http.expectOne('/api/reviews/calificacion-config').flush({ min: 1, max: 5 });
    await esperarMicrotareas();
    fixture.detectChanges();
    return fixture;
  }

  const boton = (fixture: { nativeElement: HTMLElement }, texto: string) =>
    [...fixture.nativeElement.querySelectorAll('button')].find((b) =>
      b.textContent?.includes(texto),
    );

  it('la reseña muestra la empresa y la nota sobre el máximo configurado', async () => {
    const fixture = await pintar();
    const texto = fixture.nativeElement.textContent ?? '';

    expect(texto).toContain('Grupo Ondara Software');
    // El máximo sale de la config, nunca fijado a 5 en el código.
    expect(fixture.nativeElement.querySelector('app-estrellas')?.getAttribute('aria-label')).toBe(
      '4 de 5',
    );
  });

  it('pinta los cuatro contadores del centro', async () => {
    const fixture = await pintar();
    const contadores = [...fixture.nativeElement.querySelectorAll('.c-panel__cifra')];

    expect(contadores.map((c: HTMLElement) => c.textContent)).toEqual(['12', '3', '40', '9']);
  });

  it('un resumen que falla no impide pintar el resto del panel', async () => {
    const fixture = TestBed.createComponent(PanelPage);
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA]));
    await esperarMicrotareas();
    http.expectOne('/api/panel/resumen').flush('', { status: 500, statusText: 'Error' });
    await esperarMicrotareas();
    http.expectOne('/api/reviews/pendientes').flush([PENDIENTE]);
    await esperarMicrotareas();
    http.expectOne('/api/reviews/calificacion-config').flush({ min: 1, max: 5 });
    await esperarMicrotareas();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Grupo Ondara Software');
  });

  it('aprobar saca la reseña de la lista', async () => {
    const fixture = await pintar();

    boton(fixture, 'Aprobar')?.click();
    const peticion = http.expectOne('/api/reviews/3/moderar');
    expect(peticion.request.body).toEqual({ estado: 'APROBADA', motivoRechazo: null });
    peticion.flush({ ...PENDIENTE, estado: 'APROBADA' });
    await esperarMicrotareas();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No hay reseñas pendientes.');
  });

  it('el nombre de la empresa lleva a su ficha', async () => {
    const fixture = await pintar();
    const enlace: HTMLAnchorElement = fixture.nativeElement.querySelector('.c-resena__empresa');

    expect(enlace.textContent?.trim()).toBe('Grupo Ondara Software');
    expect(enlace.getAttribute('href')).toBe('/empresas/7');
  });

  it('rechazar lleva a la cola de moderación señalando la reseña', async () => {
    const fixture = await pintar();
    const enlace = [...fixture.nativeElement.querySelectorAll('a')].find((a: HTMLAnchorElement) =>
      a.textContent?.includes('Rechazar'),
    );

    // El motivo se escribe allí: aquí no se llama a moderar (lo verifica http.verify()).
    expect(enlace?.getAttribute('href')).toBe('/reviews/pendientes?rechazar=3');
  });
});
