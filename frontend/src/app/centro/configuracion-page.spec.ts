import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { CentroService } from './centro.service';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));
// El buscador espera a que pare de teclear antes de filtrar.
const esperarTecleo = () => new Promise((resolve) => setTimeout(resolve, 300));

describe('pantalla de configuración', () => {
  let http: HttpTestingController;
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
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  const entrar = async (esAdmin: boolean) => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin, debeCambiarContrasena: false });
    await promesa;
  };

  const abrir = async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/configuracion');
    await esperarMicrotareas();
    harness.detectChanges();
    return harness;
  };

  /** Solo la pide el admin, y solo una vez abierta la pantalla. */
  const responderWhitelist = (correos: { id: number; correo: string }[] = []) =>
    http.expectOne((r) => r.url === '/api/correos-permitidos').flush(correos);

  const boton = (raiz: Element, texto: string) =>
    [...raiz.querySelectorAll('button')].find((b) => b.textContent?.includes(texto));

  const escribir = (raiz: Element, id: string, valor: string) => {
    const campo = raiz.querySelector(id) as HTMLInputElement;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  };

  it('un profesor no admin ve un aviso, no el formulario', async () => {
    await entrar(false);
    const harness = await abrir();
    const raiz = harness.routeNativeElement as Element;

    expect(raiz.textContent).toContain('Solo un administrador puede configurar el centro.');
    expect(raiz.querySelector('#centro-nombre')).toBeNull();
  });

  it('un admin ve el formulario y la whitelist se pide sola', async () => {
    await entrar(true);
    const harness = await abrir();
    responderWhitelist([{ id: 1, correo: 'ana@centro.es' }]);
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('ana@centro.es');
  });

  it('renombrar el centro actualiza la señal compartida por toda la app', async () => {
    await entrar(true);
    const harness = await abrir();
    responderWhitelist();
    await esperarMicrotareas();
    harness.detectChanges();
    const raiz = harness.routeNativeElement as Element;

    escribir(raiz, '#centro-nombre', 'IES Mi Dominio');
    harness.detectChanges();

    boton(raiz, 'Guardar')?.click();
    const peticion = http.expectOne('/api/centro');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ nombre: 'IES Mi Dominio' });
    peticion.flush({ nombre: 'IES Mi Dominio', logo: null });
    await esperarMicrotareas();

    expect(TestBed.inject(CentroService).centro().nombre).toBe('IES Mi Dominio');
  });

  it('dar de alta un correo ya existente lo dice', async () => {
    await entrar(true);
    const harness = await abrir();
    responderWhitelist();
    await esperarMicrotareas();
    harness.detectChanges();
    const raiz = harness.routeNativeElement as Element;

    escribir(raiz, '#centro-correo', 'ya@centro.es');
    harness.detectChanges();

    boton(raiz, 'Añadir')?.click();
    http
      .expectOne((r) => r.method === 'POST' && r.url === '/api/correos-permitidos')
      .flush(
        { codigo: 'CORREO_PERMITIDO_YA_EXISTE', mensaje: 'no' },
        { status: 409, statusText: 'Conflict' },
      );
    await esperarMicrotareas();
    harness.detectChanges();

    expect(raiz.textContent).toContain('Ese correo ya está en la whitelist.');
  });

  it('el buscador filtra la whitelist en el propio navegador, sin ir al servidor', async () => {
    await entrar(true);
    const harness = await abrir();
    responderWhitelist([
      { id: 1, correo: 'ana@centro.es' },
      { id: 2, correo: 'beto@centro.es' },
    ]);
    await esperarMicrotareas();
    harness.detectChanges();
    const raiz = harness.routeNativeElement as Element;

    const entrada = raiz.querySelector('.c-buscador .c-buscador__entrada') as HTMLInputElement;
    entrada.value = 'ana';
    entrada.dispatchEvent(new Event('input'));
    await esperarTecleo();
    harness.detectChanges();

    expect(raiz.textContent).toContain('ana@centro.es');
    expect(raiz.textContent).not.toContain('beto@centro.es');
  });

  it('borrar un correo pide confirmación antes de llamar al backend', async () => {
    await entrar(true);
    const harness = await abrir();
    responderWhitelist([{ id: 5, correo: 'borrame@centro.es' }]);
    await esperarMicrotareas();
    harness.detectChanges();
    const raiz = harness.routeNativeElement as Element;

    (raiz.querySelector('.c-correos__quitar') as HTMLButtonElement).click();
    harness.detectChanges();
    expect(raiz.textContent).toContain('no podrá volver a entrar');

    boton(raiz, 'Sí, quitar')?.click();
    http.expectOne('/api/correos-permitidos/5').flush(null, { status: 204, statusText: 'No Content' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(raiz.textContent).not.toContain('borrame@centro.es');
  });
});
