import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReviewService } from './review.service';
import { Review } from './review.model';

const REVIEW: Review = {
  id: 1,
  asignacionId: 10,
  empresaId: 20,
  empresaNombre: 'Beta',
  alumnoCorreo: 'alumno@centro.es',
  alumnoNombre: 'Ana Ruiz',
  autorCorreo: 'alumno@centro.es',
  contenido: 'Buena experiencia.',
  calificacion: 4,
  estado: 'PENDIENTE',
  moderadaPorCorreo: null,
  motivoRechazo: null,
  fechaCreacion: '2026-08-28T10:00:00Z',
  fechaModeracion: null,
};

describe('ReviewService', () => {
  let service: ReviewService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReviewService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('calificacionConfig consulta GET /api/reviews/calificacion-config', async () => {
    const promesa = service.calificacionConfig();
    const peticion = httpMock.expectOne('/api/reviews/calificacion-config');
    expect(peticion.request.method).toBe('GET');
    peticion.flush({ min: 1, max: 5 });
    expect(await promesa).toEqual({ min: 1, max: 5 });
  });

  it('listarPorEmpresa consulta GET /api/empresas/{id}/reviews', async () => {
    const promesa = service.listarPorEmpresa(20);
    const peticion = httpMock.expectOne('/api/empresas/20/reviews');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([REVIEW]);
    expect(await promesa).toEqual([REVIEW]);
  });

  it('crear manda POST con el request completo', async () => {
    const request = { asignacionId: 10, contenido: 'Buena experiencia.', calificacion: 4 };
    const promesa = service.crear(request);
    const peticion = httpMock.expectOne('/api/reviews');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(request);
    peticion.flush(REVIEW);
    expect(await promesa).toEqual(REVIEW);
  });

  it('editar manda PUT a /api/reviews/{id}', async () => {
    const request = { contenido: 'Editada.', calificacion: 5 };
    const promesa = service.editar(1, request);
    const peticion = httpMock.expectOne('/api/reviews/1');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual(request);
    peticion.flush({ ...REVIEW, ...request });
    await promesa;
  });

  it('listarPendientes consulta GET /api/reviews/pendientes', async () => {
    const promesa = service.listarPendientes();
    const peticion = httpMock.expectOne('/api/reviews/pendientes');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([REVIEW]);
    expect(await promesa).toEqual([REVIEW]);
  });

  it('moderar manda PUT a /api/reviews/{id}/moderar', async () => {
    const promesa = service.moderar(1, { estado: 'RECHAZADA', motivoRechazo: 'Poco detallada.' });
    const peticion = httpMock.expectOne('/api/reviews/1/moderar');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({
      estado: 'RECHAZADA',
      motivoRechazo: 'Poco detallada.',
    });
    peticion.flush({ ...REVIEW, estado: 'RECHAZADA', motivoRechazo: 'Poco detallada.' });
    await promesa;
  });
});
