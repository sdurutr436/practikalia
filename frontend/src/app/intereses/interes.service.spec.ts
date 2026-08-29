import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { InteresService } from './interes.service';
import { Interes, Interesado } from './interes.model';

const INTERES: Interes = {
  empresaId: 2,
  empresaNombre: 'Beta',
  gradoNombre: 'DAM',
  anio: 2026,
  fechaCreacion: '2026-08-29T10:00:00Z',
};

const INTERESADO: Interesado = {
  alumnoId: 10,
  alumnoCorreo: 'alumno@centro.es',
  gradoNombre: 'DAM',
  anio: 2026,
  fechaCreacion: '2026-08-29T10:00:00Z',
};

describe('InteresService', () => {
  let service: InteresService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InteresService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('marcar manda PUT sin cuerpo a /api/empresas/{id}/interes', async () => {
    const promesa = service.marcar(2);
    const peticion = httpMock.expectOne('/api/empresas/2/interes');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toBeNull();
    peticion.flush(null);
    await promesa;
  });

  it('desmarcar manda DELETE a /api/empresas/{id}/interes', async () => {
    const promesa = service.desmarcar(2);
    const peticion = httpMock.expectOne('/api/empresas/2/interes');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await promesa;
  });

  it('listarInteresados consulta GET /api/empresas/{id}/interesados', async () => {
    const promesa = service.listarInteresados(2);
    const peticion = httpMock.expectOne('/api/empresas/2/interesados');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([INTERESADO]);
    expect(await promesa).toEqual([INTERESADO]);
  });

  it('listarPorAlumno consulta GET /api/alumnos/{id}/intereses', async () => {
    const promesa = service.listarPorAlumno(10);
    const peticion = httpMock.expectOne('/api/alumnos/10/intereses');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([INTERES]);
    expect(await promesa).toEqual([INTERES]);
  });
});
