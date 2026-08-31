import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PanelPage } from './panel-page';
import { AuthService } from '../auth/auth.service';

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
    http.expectOne('/api/empresas').flush([EMPRESA]);
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

  it('rechazar sin motivo avisa sin llamar al backend', async () => {
    const fixture = await pintar();

    boton(fixture, 'Rechazar')?.click();
    fixture.detectChanges();
    boton(fixture, 'Confirmar rechazo')?.click();
    fixture.detectChanges();

    // http.verify() del afterEach falla si se hubiera llamado a moderar.
    expect(fixture.nativeElement.textContent).toContain('Indica un motivo de rechazo.');
  });

  it('rechazar con motivo manda el motivo y saca la reseña', async () => {
    const fixture = await pintar();

    boton(fixture, 'Rechazar')?.click();
    fixture.detectChanges();
    const motivo: HTMLInputElement = fixture.nativeElement.querySelector('#motivo-3');
    motivo.value = 'Falta detalle sobre las tareas.';
    boton(fixture, 'Confirmar rechazo')?.click();

    const peticion = http.expectOne('/api/reviews/3/moderar');
    expect(peticion.request.body).toEqual({
      estado: 'RECHAZADA',
      motivoRechazo: 'Falta detalle sobre las tareas.',
    });
    peticion.flush({ ...PENDIENTE, estado: 'RECHAZADA' });
    await esperarMicrotareas();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No hay reseñas pendientes.');
  });
});
