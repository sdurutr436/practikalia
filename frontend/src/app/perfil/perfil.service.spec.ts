import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PerfilService } from './perfil.service';
import { Etiqueta } from '../empresas/empresa.model';

const CATALOGO: Etiqueta[] = [
  { id: 1, nombre: 'Java' },
  { id: 2, nombre: 'Frontend' },
];

describe('PerfilService', () => {
  let service: PerfilService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PerfilService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listarEtiquetas consulta GET /api/etiquetas', async () => {
    const promesa = service.listarEtiquetas();
    const peticion = httpMock.expectOne('/api/etiquetas');
    expect(peticion.request.method).toBe('GET');
    peticion.flush(CATALOGO);
    expect(await promesa).toEqual(CATALOGO);
  });

  it('obtenerEtiquetas consulta GET /api/usuarios/{id}/etiquetas', async () => {
    const promesa = service.obtenerEtiquetas(10);
    const peticion = httpMock.expectOne('/api/usuarios/10/etiquetas');
    expect(peticion.request.method).toBe('GET');
    peticion.flush([CATALOGO[0]]);
    expect(await promesa).toEqual([CATALOGO[0]]);
  });

  it('actualizarEtiquetas manda PUT con etiquetaIds y devuelve la lista resultante', async () => {
    const promesa = service.actualizarEtiquetas(10, [1, 2]);
    const peticion = httpMock.expectOne('/api/usuarios/10/etiquetas');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ etiquetaIds: [1, 2] });
    peticion.flush(CATALOGO);
    expect(await promesa).toEqual(CATALOGO);
  });

  it('actualizarEtiquetas con lista vacía manda etiquetaIds: []', async () => {
    const promesa = service.actualizarEtiquetas(10, []);
    const peticion = httpMock.expectOne('/api/usuarios/10/etiquetas');
    expect(peticion.request.body).toEqual({ etiquetaIds: [] });
    peticion.flush([]);
    expect(await promesa).toEqual([]);
  });
});
