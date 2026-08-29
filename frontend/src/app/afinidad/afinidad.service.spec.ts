import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AfinidadService } from './afinidad.service';
import { AfinidadListado } from './afinidad.model';

const LISTADO: AfinidadListado = {
  alumnoConEtiquetas: true,
  empresas: [
    {
      empresa: {
        id: 1,
        nombre: 'Acme',
        descripcion: null,
        imagen: null,
        direccion: null,
        sector: { id: 1, nombre: 'Tecnología' },
        etiquetas: [{ id: 1, nombre: 'Java' }],
      },
      score: 0.8,
      etiquetasCoincidentes: [{ id: 1, nombre: 'Java' }],
      sectorCoincide: true,
    },
  ],
};

describe('AfinidadService', () => {
  let service: AfinidadService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AfinidadService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('propia consulta GET /api/empresas/afinidad', async () => {
    const promesa = service.propia();
    const peticion = httpMock.expectOne('/api/empresas/afinidad');
    expect(peticion.request.method).toBe('GET');
    peticion.flush(LISTADO);
    expect(await promesa).toEqual(LISTADO);
  });

  it('deAlumno consulta GET /api/alumnos/{id}/afinidad', async () => {
    const promesa = service.deAlumno(10);
    const peticion = httpMock.expectOne('/api/alumnos/10/afinidad');
    expect(peticion.request.method).toBe('GET');
    peticion.flush(LISTADO);
    expect(await promesa).toEqual(LISTADO);
  });
});
