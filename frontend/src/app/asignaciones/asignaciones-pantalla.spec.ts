import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Alumno } from '../alumnado/alumnado.service';
import { routes } from '../app.routes';
import { authInterceptor } from '../auth/auth.interceptor';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../compartido/toast/toast.service';
import { pagina } from '../pruebas';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));
/** La búsqueda espera 250 ms a que pares de teclear. */
const esperarTecleo = () => new Promise((resolve) => setTimeout(resolve, 300));

const SIN_ASIGNAR: Alumno = {
  id: 7,
  nombre: 'Lucía',
  apellido1: 'Ramírez',
  apellido2: null,
  dni: '12345678Z',
  correo: 'lucia@centro.es',
  grado: { id: 1, nombre: 'DAW' },
  anio: 2026,
  activo: true,
  empresaId: null,
  empresaNombre: null,
  tutorId: null,
  tutorNombre: null,
  tutorEmpresaId: null,
  tutorEmpresaNombre: null,
};

const ASIGNADO: Alumno = {
  ...SIN_ASIGNAR,
  id: 8,
  nombre: 'Iván',
  apellido1: 'Cabrera',
  correo: 'ivan@centro.es',
  empresaId: 3,
  empresaNombre: 'Bahía Solar',
  tutorId: 4,
  tutorNombre: 'Marta Núñez',
  tutorEmpresaId: 6,
  tutorEmpresaNombre: 'Rosa Vidal',
};

/** El claustro del desplegable de tutor de prácticas; Marta tutoriza DAW. */
const PROFESORES = [
  {
    id: 4,
    nombre: 'Marta',
    apellido1: 'Núñez',
    apellido2: null,
    dni: '12345678Z',
    correo: 'marta@centro.es',
    esAdmin: false,
    clase: { id: 1, nombre: 'DAW' },
    alumnosPractica: 1,
  },
];

const EMPRESAS = [
  { id: 3, nombre: 'Bahía Solar', tutores: [] },
  {
    id: 5,
    nombre: 'Acme',
    tutores: [{ id: 6, nombre: 'Rosa Vidal', cargo: null, telefono: null, correo: null }],
  },
];

const CURSOS = { actual: 2026, cursos: [2026, 2025] };

describe('pantalla de asignaciones', () => {
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

  const entrar = async () => {
    const promesa = auth.login('profesor@centro.es', 'secreta', '');
    http
      .expectOne('/api/auth/login')
      .flush({ rol: 'PROFESOR', esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  };

  /** Los catálogos que la pantalla pide al arrancar, en paralelo. */
  const catalogos = () => {
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina(EMPRESAS));
    http.expectOne('/api/grados/publico').flush([{ id: 1, nombre: 'DAW' }]);
    http.expectOne('/api/alumnos/cursos').flush(CURSOS);
    http.expectOne((r) => r.url === '/api/profesores').flush(pagina(PROFESORES));
  };

  const abrir = async (url: string, alumnos: Alumno[]) => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    catalogos();
    http.expectOne((r) => r.url === '/api/alumnos/curso').flush(pagina(alumnos));
    await esperarMicrotareas();
    harness.detectChanges();
    return harness;
  };

  // Dentro de las filas: en la barra de filtros hay otros dos desplegables.
  const entradas = (raiz: Element) => [
    ...raiz.querySelectorAll<HTMLInputElement>('.c-asignacion .c-desplegable__entrada'),
  ];

  /** El primer desplegable de cada fila, que es el de empresa. */
  const empresas = (raiz: Element) =>
    [...raiz.querySelectorAll('.c-asignacion')].map(
      (fila) => fila.querySelector<HTMLInputElement>('.c-desplegable__entrada')!.value,
    );

  const opciones = (raiz: Element) => [
    ...raiz.querySelectorAll<HTMLElement>('.c-asignacion .c-desplegable__opcion'),
  ];

  const guardarDe = (raiz: Element) =>
    raiz.querySelector<HTMLButtonElement>('.c-asignacion .c-boton')!;

  /** Enfocar abre la lista; teclear la filtra. */
  const buscar = (entrada: HTMLInputElement, texto: string) => {
    entrada.dispatchEvent(new Event('focus'));
    entrada.value = texto;
    entrada.dispatchEvent(new Event('input'));
  };

  it('solo pide el catálogo de empresas confirmadas', async () => {
    await entrar();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/asignaciones');
    const peticion = http.expectOne((r) => r.url === '/api/empresas');
    expect(peticion.request.params.get('publicada')).toBe('true');
    peticion.flush(pagina(EMPRESAS));
    http.expectOne('/api/grados/publico').flush([]);
    http.expectOne((r) => r.url === '/api/profesores').flush(pagina(PROFESORES));
    http.expectOne('/api/alumnos/cursos').flush(CURSOS);
    http.expectOne((r) => r.url === '/api/alumnos/curso').flush(pagina([]));
    await esperarMicrotareas();
  });

  it('pinta una fila por alumno con la empresa que ya tiene', async () => {
    await entrar();
    const harness = await abrir('/asignaciones', [SIN_ASIGNAR, ASIGNADO]);

    const raiz = harness.routeNativeElement!;
    expect(empresas(raiz)).toEqual(['', 'Bahía Solar']);
    // El tutor de prácticas por defecto es el tutor de su clase (Marta, de DAW).
    expect(entradas(raiz)[1].value).toBe('Marta Núñez');
  });

  it('la pastilla «Sin asignar» filtra por asignado=false', async () => {
    await entrar();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/asignaciones?estado=sin-asignar');
    catalogos();
    const peticion = http.expectOne((r) => r.url === '/api/alumnos/curso');
    expect(peticion.request.params.get('asignado')).toBe('false');
    peticion.flush(pagina([SIN_ASIGNAR]));
    await esperarMicrotareas();
  });

  it('la búsqueda y los filtros viajan en la URL', async () => {
    await entrar();
    const harness = await abrir('/asignaciones?texto=ram&gradoId=1&anio=2025', [SIN_ASIGNAR]);
    const raiz = harness.routeNativeElement!;

    // La barra arranca con lo que trae la URL, no vacía.
    const barra = raiz.querySelector<HTMLInputElement>('.c-buscador__entrada')!;
    expect(barra.value).toBe('ram');

    barra.value = 'cabrera';
    barra.dispatchEvent(new Event('input'));
    await esperarTecleo();

    const peticion = http.expectOne((r) => r.url === '/api/alumnos/curso');
    expect(peticion.request.params.get('texto')).toBe('cabrera');
    expect(peticion.request.params.get('gradoId')).toBe('1');
    expect(peticion.request.params.get('anio')).toBe('2025');
    peticion.flush(pagina([]));
    await esperarMicrotareas();
  });

  it('la búsqueda del desplegable ignora los acentos', async () => {
    await entrar();
    const harness = await abrir('/asignaciones', [SIN_ASIGNAR]);
    const raiz = harness.routeNativeElement!;

    buscar(entradas(raiz)[0], 'bahia');
    harness.detectChanges();

    expect(opciones(raiz).map((o) => o.textContent!.trim())).toEqual(['Bahía Solar']);
  });

  it('elegir una empresa y guardar manda su id, y lo confirma el toast', async () => {
    await entrar();
    const harness = await abrir('/asignaciones', [SIN_ASIGNAR]);
    const raiz = harness.routeNativeElement!;

    expect(guardarDe(raiz).disabled).toBe(true);

    buscar(entradas(raiz)[0], 'acme');
    harness.detectChanges();
    opciones(raiz)[0].click();
    harness.detectChanges();

    expect(guardarDe(raiz).disabled).toBe(false);
    guardarDe(raiz).click();

    const peticion = http.expectOne(`/api/alumnos/${SIN_ASIGNAR.id}/asignacion`);
    expect(peticion.request.method).toBe('PUT');
    // Los dos tutores van por defecto: el de su clase y el primero de la empresa.
    expect(peticion.request.body).toEqual({
      empresaId: 5,
      tutorCentroId: 4,
      tutorEmpresaId: 6,
    });
    peticion.flush({ empresaNombre: 'Acme' });
    await esperarMicrotareas();

    http
      .expectOne((r) => r.url === '/api/alumnos/curso')
      .flush(pagina([{ ...SIN_ASIGNAR, empresaId: 5, empresaNombre: 'Acme' }]));
    await esperarMicrotareas();
    harness.detectChanges();

    expect(entradas(raiz)[0].value).toBe('Acme');

    // El aviso sale en el toast del marco, no en la pantalla.
    const toast = TestBed.inject(ToastService);
    expect(toast.mensaje()).toContain('Acme');
    toast.cerrar();
    expect(toast.mensaje()).toBeNull();
  });

  it('no deja guardar la empresa que el alumno ya tiene', async () => {
    await entrar();
    const harness = await abrir('/asignaciones', [ASIGNADO]);
    const raiz = harness.routeNativeElement!;

    expect(guardarDe(raiz).disabled).toBe(true);

    buscar(entradas(raiz)[0], '');
    harness.detectChanges();
    opciones(raiz)[0].click();
    harness.detectChanges();

    expect(guardarDe(raiz).disabled).toBe(true);
  });
});
