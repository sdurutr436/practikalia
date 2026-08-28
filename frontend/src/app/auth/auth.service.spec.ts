import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

const LOGIN_URL = '/api/auth/login';
const SESION_ALUMNO = { rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false };

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('login guarda la sesión devuelta', async () => {
    const promesa = service.login('alumno@centro.es', 'secreta', '');
    const peticion = httpMock.expectOne(LOGIN_URL);
    expect(peticion.request.body).toEqual({
      correo: 'alumno@centro.es',
      contrasena: 'secreta',
      web: '',
    });
    peticion.flush(SESION_ALUMNO);
    const sesion = await promesa;
    expect(sesion.rol).toBe('ALUMNO');
    expect(sesion.id).toBeNull();
    expect(service.sesion()?.debeCambiarContrasena).toBe(false);
  });

  it('login reintenta una sola vez ante el 403 genérico de CSRF', async () => {
    const promesa = service.login('a@b.es', 'x', '');
    httpMock
      .expectOne(LOGIN_URL)
      .flush(
        { codigo: 'ACCESO_DENEGADO', mensaje: 'Acceso denegado' },
        { status: 403, statusText: 'Forbidden' },
      );
    httpMock.expectOne(LOGIN_URL).flush({ ...SESION_ALUMNO, rol: 'PROFESOR', esAdmin: true });
    const sesion = await promesa;
    expect(sesion.esAdmin).toBe(true);
  });

  it('login no reintenta un 403 de negocio (CUENTA_NO_DISPONIBLE)', async () => {
    const promesa = service.login('a@b.es', 'x', '');
    httpMock
      .expectOne(LOGIN_URL)
      .flush(
        { codigo: 'CUENTA_NO_DISPONIBLE', mensaje: 'Cuenta no disponible' },
        { status: 403, statusText: 'Forbidden' },
      );
    await expect(promesa).rejects.toMatchObject({ status: 403 });
    expect(service.sesion()).toBeNull();
  });

  it('login con credenciales inválidas propaga el 401 sin fijar sesión', async () => {
    const promesa = service.login('a@b.es', 'mal', '');
    httpMock
      .expectOne(LOGIN_URL)
      .flush(
        { codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'Credenciales inválidas' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await expect(promesa).rejects.toMatchObject({ status: 401 });
    expect(service.sesion()).toBeNull();
  });

  it('logout limpia la sesión aunque la petición falle', async () => {
    const promesaLogin = service.login('a@b.es', 'x', '');
    httpMock.expectOne(LOGIN_URL).flush(SESION_ALUMNO);
    await promesaLogin;

    const promesa = service.logout().catch(() => undefined);
    httpMock
      .expectOne('/api/auth/logout')
      .flush({ codigo: 'NO_AUTENTICADO' }, { status: 401, statusText: 'Unauthorized' });
    await promesa;
    expect(service.sesion()).toBeNull();
  });

  it('asegurarSesion rehidrata con /me una sola vez', async () => {
    const promesa = service.asegurarSesion();
    httpMock.expectOne('/api/auth/me').flush({
      id: 7,
      correo: 'a@b.es',
      ...SESION_ALUMNO,
      etiquetas: [],
    });
    const sesion = await promesa;
    expect(sesion?.correo).toBe('a@b.es');
    expect(sesion?.id).toBe(7);
    // Segunda llamada: responde de memoria, sin petición nueva (verify() lo comprueba).
    expect(await service.asegurarSesion()).not.toBeNull();
  });

  it('asegurarSesion sin cookie válida devuelve null y no reintenta', async () => {
    const promesa = service.asegurarSesion();
    httpMock
      .expectOne('/api/auth/me')
      .flush({ codigo: 'NO_AUTENTICADO' }, { status: 401, statusText: 'Unauthorized' });
    expect(await promesa).toBeNull();
    expect(await service.asegurarSesion()).toBeNull();
  });

  it('cambiarContrasena desactiva el flag de cambio pendiente', async () => {
    const promesaLogin = service.login('a@b.es', 'temporal', '');
    httpMock.expectOne(LOGIN_URL).flush({ ...SESION_ALUMNO, debeCambiarContrasena: true });
    await promesaLogin;

    const promesa = service.cambiarContrasena('temporal', 'Nueva.1234');
    const peticion = httpMock.expectOne('/api/auth/cambiar-contrasena');
    expect(peticion.request.body).toEqual({
      contrasenaActual: 'temporal',
      contrasenaNueva: 'Nueva.1234',
    });
    peticion.flush(null, { status: 204, statusText: 'No Content' });
    await promesa;
    expect(service.sesion()?.debeCambiarContrasena).toBe(false);
  });
});
