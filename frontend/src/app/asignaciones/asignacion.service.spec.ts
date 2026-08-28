import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AsignacionService } from './asignacion.service';
import { Asignacion, CrearAsignacionRequest } from './asignacion.model';

const ASIGNACION: Asignacion = {
  id: 1,
  alumnoId: 10,
  alumnoCorreo: 'alumno@centro.es',
  empresaId: 20,
  empresaNombre: 'Acme',
  tutorCentroId: 30,
  tutorCentroCorreo: 'profesor@centro.es',
  grado: { id: 40, nombre: 'DAM' },
  anio: 2026,
  fechaInicio: '2026-09-01',
  fechaFin: null,
  contratadoPosterior: null,
};

const REQUEST: CrearAsignacionRequest = {
  alumnoId: 10,
  empresaId: 20,
  tutorCentroId: 30,
  gradoId: 40,
  anio: 2026,
  fechaInicio: '2026-09-01',
};

describe('AsignacionService', () => {
  let service: AsignacionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AsignacionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listarPorEmpresa consulta GET /api/empresas/{id}/asignaciones', async () => {
    const promesa = service.listarPorEmpresa(20);
    const peticion = httpMock.expectOne('/api/empresas/20/asignaciones');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([ASIGNACION]);
    expect(await promesa).toEqual([ASIGNACION]);
  });

  it('listarPorAlumno consulta GET /api/alumnos/{id}/asignaciones', async () => {
    const promesa = service.listarPorAlumno(10);
    const peticion = httpMock.expectOne('/api/alumnos/10/asignaciones');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([ASIGNACION]);
    expect(await promesa).toEqual([ASIGNACION]);
  });

  it('crear manda POST con el request completo', async () => {
    const promesa = service.crear(REQUEST);
    const peticion = httpMock.expectOne('/api/asignaciones');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(REQUEST);
    peticion.flush(ASIGNACION);
    expect(await promesa).toEqual(ASIGNACION);
  });

  it('cerrar manda PUT a /api/asignaciones/{id}', async () => {
    const promesa = service.cerrar(1, { fechaFin: '2027-06-30', contratadoPosterior: true });
    const peticion = httpMock.expectOne('/api/asignaciones/1');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ fechaFin: '2027-06-30', contratadoPosterior: true });
    peticion.flush({ ...ASIGNACION, fechaFin: '2027-06-30', contratadoPosterior: true });
    await promesa;
  });

  it('listarGrados consulta GET /api/grados', async () => {
    const promesa = service.listarGrados();
    const peticion = httpMock.expectOne('/api/grados');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([{ id: 40, nombre: 'DAM' }]);
    expect(await promesa).toEqual([{ id: 40, nombre: 'DAM' }]);
  });

  it('listarUsuarios consulta GET /api/usuarios con el filtro de rol', async () => {
    const promesa = service.listarUsuarios('ALUMNO');
    const peticion = httpMock.expectOne('/api/usuarios?rol=ALUMNO');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([{ id: 10, correo: 'alumno@centro.es', rol: 'ALUMNO' }]);
    expect(await promesa).toEqual([{ id: 10, correo: 'alumno@centro.es', rol: 'ALUMNO' }]);
  });
});
