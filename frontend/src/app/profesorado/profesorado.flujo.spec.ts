import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { ToastService } from '../compartido/toast/toast.service';
import { pagina } from '../pruebas';
import { Alumno } from '../alumnado/alumnado.service';
import { Profesor } from './profesorado.service';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const TUTORA: Profesor = {
  id: 4,
  nombre: 'Marta',
  apellido1: 'Núñez',
  apellido2: 'Gil',
  dni: '12345678Z',
  correo: 'marta@centro.es',
  esAdmin: false,
  clase: { id: 1, nombre: 'DAW' },
  alumnosPractica: 2,
};

const ADMINISTRADORA: Profesor = {
  ...TUTORA,
  id: 5,
  nombre: 'Ana',
  apellido1: 'Soler',
  apellido2: null,
  correo: 'ana@centro.es',
  esAdmin: true,
  clase: null,
  alumnosPractica: 0,
};

const ALUMNO_ASIGNADO: Alumno = {
  id: 8,
  nombre: 'Iván',
  apellido1: 'Cabrera',
  apellido2: null,
  dni: '87654321X',
  correo: 'ivan@centro.es',
  grado: { id: 1, nombre: 'DAW' },
  anio: 2026,
  activo: true,
  empresaId: 3,
  empresaNombre: 'Bahía Solar',
  tutorId: 9,
  tutorNombre: 'Otro Tutor',
};

describe('listado de profesorado', () => {
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

  const abrir = async (url: string, profesores: Profesor[]) => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    http.expectOne((r) => r.url === '/api/profesores').flush(pagina(profesores));
    await esperarMicrotareas();
    harness.detectChanges();
    return harness;
  };

  /** Los dos catálogos que pide la ficha la primera vez que se abre. */
  const responderCatalogos = (alumnos: Alumno[] = [ALUMNO_ASIGNADO]) => {
    http.expectOne('/api/grados/publico').flush([{ id: 1, nombre: 'DAW' }]);
    http.expectOne((r) => r.url === '/api/alumnos/curso').flush(pagina(alumnos));
  };

  const boton = (raiz: Element, texto: string) =>
    [...raiz.querySelectorAll('button')].find((b) => b.textContent?.includes(texto));

  const escribir = (raiz: Element, id: string, valor: string) => {
    const campo = raiz.querySelector(id) as HTMLInputElement;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  };

  it('un alumno no puede entrar al listado', async () => {
    const promesa = auth.login('alumno@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'ALUMNO', esAdmin: false, debeCambiarContrasena: false });
    await promesa;

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profesorado');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('la tarjeta enseña nombre, correo, la clase que tutoriza y el estatus', async () => {
    await entrar(false);
    const harness = await abrir('/profesorado', [TUTORA, ADMINISTRADORA]);
    const texto = harness.routeNativeElement?.textContent ?? '';

    expect(texto).toContain('Marta Núñez Gil');
    expect(texto).toContain('marta@centro.es');
    expect(texto).toContain('Tutor de DAW');
    expect(texto).toContain('2 alumnos de prácticas');
    expect(texto).toContain('Profesor');
    expect(texto).toContain('Administrador');
    expect(texto).toContain('Sin clase asignada');
  });

  it('el profesorado no se edita entre sí: sin admin no hay lápiz ni alta', async () => {
    await entrar(false);
    const harness = await abrir('/profesorado', [TUTORA]);
    const raiz = harness.routeNativeElement as Element;

    expect(boton(raiz, 'Editar')).toBeUndefined();
    expect(boton(raiz, 'Nuevo profesor')).toBeUndefined();
  });

  it('la pastilla «Sin clase» pide conClase=false', async () => {
    await entrar(false);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/profesorado?estado=sin-clase');
    const carga = http.expectOne((r) => r.url === '/api/profesores');
    expect(carga.request.params.get('conClase')).toBe('false');
    carga.flush(pagina([ADMINISTRADORA]));
    await esperarMicrotareas();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Sin clase asignada');
  });

  it('el admin da de alta y se le recuerda cuál es la contraseña', async () => {
    await entrar(true);
    const harness = await abrir('/profesorado', []);
    const raiz = harness.routeNativeElement as Element;

    boton(raiz, 'Nuevo profesor')?.click();
    responderCatalogos();
    await esperarMicrotareas();
    harness.detectChanges();

    escribir(raiz, '#profesor-nombre', 'Marta');
    escribir(raiz, '#profesor-apellido1', 'Núñez');
    escribir(raiz, '#profesor-dni', '12345678Z');
    escribir(raiz, '#profesor-correo', 'marta@centro.es');
    harness.detectChanges();

    boton(raiz, 'Crear profesor')?.click();
    const peticion = http.expectOne('/api/profesores');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body.correo).toBe('marta@centro.es');
    expect(peticion.request.body.esAdmin).toBe(false);
    expect(peticion.request.body.alumnosPractica).toEqual([]);
    peticion.flush(TUTORA);
    await esperarMicrotareas();

    http.expectOne((r) => r.url === '/api/profesores').flush(pagina([TUTORA]));
    await esperarMicrotareas();
    expect(TestBed.inject(ToastService).mensaje()).toContain('su DNI sin la letra');
  });

  it('darle un alumno de prácticas manda su id en la ficha', async () => {
    await entrar(true);
    const harness = await abrir('/profesorado', [TUTORA]);
    const raiz = harness.routeNativeElement as Element;

    boton(raiz, 'Editar')?.click();
    responderCatalogos();
    await esperarMicrotareas();
    harness.detectChanges();

    // El desplegable se abre al enfocarlo, se filtra tecleando y elige con un clic.
    // El id va al anfitrión del componente; quien escucha es su <input> de dentro.
    const entrada = raiz.querySelector(
      '#profesor-practicas .c-desplegable__entrada',
    ) as HTMLInputElement;
    entrada.dispatchEvent(new Event('focus'));
    entrada.value = 'ivan';
    entrada.dispatchEvent(new Event('input'));
    harness.detectChanges();
    const opcion = raiz.querySelector('.c-desplegable__opcion') as HTMLElement;
    expect(opcion.textContent).toContain('Iván Cabrera');
    expect(opcion.textContent).toContain('Otro Tutor');
    opcion.click();
    harness.detectChanges();

    boton(raiz, 'Guardar cambios')?.click();
    const peticion = http.expectOne('/api/profesores/4');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body.alumnosPractica).toEqual([8]);
    peticion.flush({ ...TUTORA, alumnosPractica: 3 });
    await esperarMicrotareas();

    http.expectOne((r) => r.url === '/api/profesores').flush(pagina([TUTORA]));
    await esperarMicrotareas();
  });

  it('quitarle el permiso al último administrador se explica en la ficha', async () => {
    await entrar(true);
    const harness = await abrir('/profesorado', [ADMINISTRADORA]);
    const raiz = harness.routeNativeElement as Element;

    boton(raiz, 'Editar')?.click();
    responderCatalogos();
    await esperarMicrotareas();
    harness.detectChanges();

    const casilla = raiz.querySelector('input[type="checkbox"]') as HTMLInputElement;
    expect(casilla.checked).toBe(true);
    casilla.click();
    harness.detectChanges();

    boton(raiz, 'Guardar cambios')?.click();
    http
      .expectOne('/api/profesores/5')
      .flush({ codigo: 'ULTIMO_ADMINISTRADOR', mensaje: 'no' }, { status: 409, statusText: 'Conflict' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(raiz.textContent).toContain('único administrador');
  });
});
