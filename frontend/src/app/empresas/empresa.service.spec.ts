import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmpresaService } from './empresa.service';
import { Empresa, EmpresaRequest } from './empresa.model';

const EMPRESA_ALUMNO: Empresa = {
  id: 1,
  nombre: 'Acme',
  descripcion: 'Descripción',
  imagen: null,
  direccion: 'Calle Falsa 123',
  sector: { id: 10, nombre: 'Informática' },
  etiquetas: [{ id: 20, nombre: 'Remoto' }],
};

const REQUEST: EmpresaRequest = {
  nombre: 'Acme',
  descripcion: 'Descripción',
  direccion: 'Calle Falsa 123',
  sectorId: 10,
  etiquetaIds: [20],
  observaciones: '',
  contactoNombre: '',
  contactoTelefono: '',
  contactoEmail: '',
  publicada: false,
};

describe('EmpresaService', () => {
  let service: EmpresaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EmpresaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listar consulta GET /api/empresas', async () => {
    const promesa = service.listar();
    const peticion = httpMock.expectOne('/api/empresas');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([EMPRESA_ALUMNO]);
    expect(await promesa).toEqual([EMPRESA_ALUMNO]);
  });

  it('obtener consulta GET /api/empresas/{id}', async () => {
    const promesa = service.obtener(1);
    const peticion = httpMock.expectOne('/api/empresas/1');
    expect(peticion.request.method).toBe('GET');
    peticion.flush(EMPRESA_ALUMNO);
    expect(await promesa).toEqual(EMPRESA_ALUMNO);
  });

  it('crear manda POST con el request completo', async () => {
    const promesa = service.crear(REQUEST);
    const peticion = httpMock.expectOne('/api/empresas');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(REQUEST);
    peticion.flush({ ...EMPRESA_ALUMNO, publicada: false });
    await promesa;
  });

  it('actualizar manda PUT a /api/empresas/{id}', async () => {
    const promesa = service.actualizar(1, { ...REQUEST, publicada: true });
    const peticion = httpMock.expectOne('/api/empresas/1');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.publicada).toBe(true);
    peticion.flush({ ...EMPRESA_ALUMNO, publicada: true });
    await promesa;
  });

  it('subirImagen manda POST multipart con el campo fichero', async () => {
    const fichero = new File(['contenido'], 'foto.jpg', { type: 'image/jpeg' });
    const promesa = service.subirImagen(1, fichero);
    const peticion = httpMock.expectOne('/api/empresas/1/imagen');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body instanceof FormData).toBe(true);
    expect((peticion.request.body as FormData).get('fichero')).toBe(fichero);
    peticion.flush({ ...EMPRESA_ALUMNO, imagen: '/uploads/empresas/foto.jpg' });
    const empresa = await promesa;
    expect(empresa.imagen).toBe('/uploads/empresas/foto.jpg');
  });
});
