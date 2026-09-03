import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { ToastService } from '../compartido/toast/toast.service';
import { Nodo } from './catalogo.service';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const hoja = (id: number, nombre: string): Nodo => ({ id, nombre, transversal: false, hijas: [] });

const INFORMATICA: Nodo = {
  id: 1,
  nombre: 'Informática y comunicaciones',
  transversal: false,
  hijas: [
    { id: 2, nombre: 'Desarrollo web', transversal: false, hijas: [hoja(3, 'Java')] },
    hoja(4, 'Ciberseguridad'),
  ],
};

const MODALIDAD: Nodo = {
  id: 5,
  nombre: 'Modalidad de trabajo',
  transversal: true,
  hijas: [hoja(6, 'Teletrabajo')],
};

const ARBOL = [INFORMATICA, MODALIDAD];

describe('catálogo de sectores y etiquetas', () => {
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

  const abrir = async (url = '/sectores', arbol: Nodo[] = ARBOL) => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    http.expectOne('/api/etiquetas/arbol').flush(arbol);
    await esperarMicrotareas();
    harness.detectChanges();
    return harness;
  };

  const raiz = (harness: RouterTestingHarness) => harness.routeNativeElement as Element;

  const columnas = (harness: RouterTestingHarness) => [
    ...raiz(harness).querySelectorAll('.c-catalogo'),
  ];

  const pulsar = (dentro: Element, etiqueta: string) =>
    [...dentro.querySelectorAll('button')]
      .find(
        (boton) =>
          boton.getAttribute('aria-label') === etiqueta ||
          boton.textContent?.trim().startsWith(etiqueta),
      )
      ?.click();

  const escribir = (dentro: Element, valor: string) => {
    const entrada = dentro.querySelector('.c-catalogo__alta input') as HTMLInputElement;
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
  };

  it('un profesor sin esAdmin no entra', async () => {
    await entrar(false);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/sectores');
    http
      .expectOne((r) => r.url === '/api/empresas')
      .flush({
        contenido: [],
        pagina: 0,
        tamano: 0,
        total: 0,
        paginas: 1,
      });
    await esperarMicrotareas();

    expect(router.url).toBe('/empresas');
  });

  it('la primera columna lista sectores y grupos transversales por separado', async () => {
    await entrar(true);
    const harness = await abrir();
    const primera = columnas(harness)[0];

    expect(primera.textContent).toContain('Informática y comunicaciones');
    expect(primera.textContent).toContain('Modalidad de trabajo');
    // Solo los transversales llevan rótulo: marca dónde dejan de ser sectores.
    expect([...primera.querySelectorAll('.c-catalogo__grupo')].map((g) => g.textContent)).toEqual([
      'Transversales',
    ]);
  });

  it('las columnas de la derecha nacen apagadas y se encadenan al elegir', async () => {
    await entrar(true);
    const harness = await abrir();
    expect(
      columnas(harness).filter((c) => c.classList.contains('c-catalogo--apagada')),
    ).toHaveLength(2);

    pulsar(columnas(harness)[0], 'Informática y comunicaciones');
    await esperarMicrotareas();
    harness.detectChanges();

    expect(columnas(harness)[1].textContent).toContain('Desarrollo web');
    expect(columnas(harness)[2].classList).toContain('c-catalogo--apagada');

    pulsar(columnas(harness)[1], 'Desarrollo web');
    await esperarMicrotareas();
    harness.detectChanges();

    expect(columnas(harness)[2].textContent).toContain('Java');
  });

  it('un grupo transversal enseña sus etiquetas y no ofrece un tercer nivel', async () => {
    await entrar(true);
    const harness = await abrir('/sectores?sector=5');

    expect(columnas(harness)[1].textContent).toContain('Etiquetas transversales');
    expect(columnas(harness)[1].textContent).toContain('Teletrabajo');
    expect(columnas(harness)[2].classList).toContain('c-catalogo--apagada');
  });

  it('la etiqueta nueva cuelga de la actividad elegida y se avisa con un toast', async () => {
    await entrar(true);
    const harness = await abrir('/sectores?sector=1&actividad=2');

    escribir(columnas(harness)[2], 'Angular');
    harness.detectChanges();
    pulsar(columnas(harness)[2], 'Añadir a Etiquetas');

    const alta = http.expectOne('/api/etiquetas');
    expect(alta.request.method).toBe('POST');
    expect(alta.request.body).toEqual({ nombre: 'Angular', padreId: 2, transversal: false });
    alta.flush(hoja(9, 'Angular'));
    await esperarMicrotareas();

    http.expectOne('/api/etiquetas/arbol').flush(ARBOL);
    await esperarMicrotareas();

    expect(TestBed.inject(ToastService).mensaje()).toContain('Angular');
  });

  it('el alta de la primera columna crea un grupo transversal si se marca la casilla', async () => {
    await entrar(true);
    const harness = await abrir();
    const primera = columnas(harness)[0];

    const casilla = primera.querySelector('.c-catalogo__marca input') as HTMLInputElement;
    casilla.checked = true;
    casilla.dispatchEvent(new Event('change'));
    escribir(primera, 'Jornada');
    harness.detectChanges();
    pulsar(columnas(harness)[0], 'Añadir a Sectores');

    const alta = http.expectOne('/api/etiquetas');
    expect(alta.request.body).toEqual({ nombre: 'Jornada', padreId: null, transversal: true });
    alta.flush({ id: 9, nombre: 'Jornada', transversal: true, hijas: [] });
    await esperarMicrotareas();
    http.expectOne('/api/etiquetas/arbol').flush(ARBOL);
    await esperarMicrotareas();
  });

  it('el borrado pide confirmación, explica el rechazo y no suelta lo elegido', async () => {
    await entrar(true);
    const harness = await abrir('/sectores?sector=1');

    pulsar(columnas(harness)[0], 'Borrar Informática y comunicaciones');
    harness.detectChanges();
    expect(columnas(harness)[0].textContent).toContain('¿Borrar «Informática y comunicaciones»?');

    pulsar(columnas(harness)[0], 'Sí, borrar Informática y comunicaciones');
    http
      .expectOne('/api/etiquetas/1')
      .flush(
        { codigo: 'ETIQUETA_CON_HIJAS', mensaje: '' },
        { status: 409, statusText: 'Conflict' },
      );
    await esperarMicrotareas();
    harness.detectChanges();

    expect(raiz(harness).textContent).toContain('vacíalo antes de borrarlo');
    // Rechazado el borrado, el sector sigue elegido: hay que poder vaciarlo.
    expect(router.url).toContain('sector=1');
    expect(columnas(harness)[1].textContent).toContain('Desarrollo web');
  });
});
