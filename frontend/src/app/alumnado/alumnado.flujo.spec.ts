import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { pagina } from '../pruebas';
import { Alumno } from './alumnado.service';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const PENDIENTE: Alumno = {
  id: 7,
  nombre: 'Lucía',
  apellido1: 'Ramírez',
  apellido2: 'Ortega',
  dni: '12345678Z',
  correo: 'lucia@centro.es',
  grado: { id: 1, nombre: 'DAW' },
  anio: 2026,
  activo: false,
  empresaId: null,
  empresaNombre: null,
};

const ASIGNADO: Alumno = {
  ...PENDIENTE,
  id: 8,
  nombre: 'Iván',
  apellido1: 'Cabrera',
  apellido2: null,
  correo: 'ivan@centro.es',
  activo: true,
  empresaId: 3,
  empresaNombre: 'Bahía Solar',
};

describe('listado de alumnado', () => {
  let http: HttpTestingController;
  let router: Router;
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
    router = TestBed.inject(Router);
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

  /** La pantalla pide el catálogo de clases y la página a la vez al arrancar. */
  const abrir = async (url: string, alumnos: Alumno[]) => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    http.expectOne('/api/grados/publico').flush([{ id: 1, nombre: 'DAW' }]);
    http.expectOne((r) => r.url === '/api/alumnos').flush(pagina(alumnos));
    await esperarMicrotareas();
    harness.detectChanges();
    return harness;
  };

  const boton = (raiz: Element, texto: string) =>
    [...raiz.querySelectorAll('button')].find((b) => b.textContent?.includes(texto));

  it('un alumno no puede entrar al listado', async () => {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/alumnado');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('la tarjeta enseña nombre, correo, clase y la empresa asignada', async () => {
    await entrar(false);
    const harness = await abrir('/alumnado', [ASIGNADO]);
    const texto = harness.routeNativeElement?.textContent ?? '';

    expect(texto).toContain('Iván Cabrera');
    expect(texto).toContain('ivan@centro.es');
    expect(texto).toContain('DAW');
    expect(texto).toContain('Bahía Solar');
  });

  it('sin asignación abierta la pastilla dice «Sin asignar»', async () => {
    await entrar(false);
    const harness = await abrir('/alumnado', [PENDIENTE]);
    expect(harness.routeNativeElement?.textContent).toContain('Sin asignar');
  });

  it('la pastilla «Por confirmar» pide activo=false', async () => {
    await entrar(false);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/alumnado?estado=por-confirmar');
    http.expectOne('/api/grados/publico').flush([]);
    const carga = http.expectOne((r) => r.url === '/api/alumnos');
    expect(carga.request.params.get('activo')).toBe('false');
    carga.flush(pagina([PENDIENTE]));
    await esperarMicrotareas();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Por confirmar');
  });

  it('confirmar solo se ofrece al admin', async () => {
    await entrar(false);
    const harness = await abrir('/alumnado', [PENDIENTE]);
    expect(boton(harness.routeNativeElement as Element, 'Confirmar alumno')).toBeUndefined();
  });

  it('el admin confirma y se le recuerda cuál es la contraseña', async () => {
    await entrar(true);
    const harness = await abrir('/alumnado', [PENDIENTE]);

    boton(harness.routeNativeElement as Element, 'Confirmar alumno')?.click();
    const peticion = http.expectOne('/api/usuarios/7/activar');
    expect(peticion.request.method).toBe('PUT');
    peticion.flush(null);
    await esperarMicrotareas();

    http.expectOne((r) => r.url === '/api/alumnos').flush(pagina([{ ...PENDIENTE, activo: true }]));
    await esperarMicrotareas();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('su DNI sin la letra');
  });

  it('editar la ficha manda el PUT con los datos del formulario', async () => {
    await entrar(false);
    const harness = await abrir('/alumnado', [PENDIENTE]);
    const raiz = harness.routeNativeElement as Element;

    boton(raiz, 'Editar')?.click();
    harness.detectChanges();

    const correo = raiz.querySelector('#alumno-correo') as HTMLInputElement;
    expect(correo.value).toBe('lucia@centro.es');
    correo.value = 'nuevo@centro.es';
    correo.dispatchEvent(new Event('input'));
    harness.detectChanges();

    boton(raiz, 'Guardar cambios')?.click();
    const peticion = http.expectOne('/api/alumnos/7');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.correo).toBe('nuevo@centro.es');
    expect(peticion.request.body.dni).toBe('12345678Z');
    peticion.flush({ ...PENDIENTE, correo: 'nuevo@centro.es' });
    await esperarMicrotareas();

    http.expectOne((r) => r.url === '/api/alumnos').flush(pagina([]));
    await esperarMicrotareas();
  });

  it('un CSV rechazado no importa a nadie y lo dice', async () => {
    await entrar(false);
    const harness = await abrir('/alumnado', []);
    const raiz = harness.routeNativeElement as Element;

    const selector = raiz.querySelector('input[type="file"]') as HTMLInputElement;
    const fichero = new File(['nombre,apellido1\n'], 'alumnado.csv', { type: 'text/csv' });
    Object.defineProperty(selector, 'files', { value: [fichero], configurable: true });
    selector.dispatchEvent(new Event('change'));
    await esperarMicrotareas();

    http
      .expectOne('/api/alumnos/importar')
      .flush(
        { codigo: 'CSV_INVALIDO', mensaje: 'Línea 3: el DNI no es válido' },
        { status: 400, statusText: 'Bad Request' },
      );
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Línea 3');
  });
});
