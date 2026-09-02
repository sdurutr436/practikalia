import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { Empresa } from './empresa.model';
import { EmpresaFormularioPage } from './empresa-formulario-page/empresa-formulario-page';
import { pagina } from '../pruebas';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));
const esperarTecleo = () => new Promise((resolve) => setTimeout(resolve, 300));

const EMPRESA_PUBLICADA: Empresa = {
  id: 1,
  nombre: 'Acme',
  descripcion: 'Una empresa',
  imagen: null,
  direccion: 'Calle Falsa 123',
  sector: { id: 10, nombre: 'Informática' },
  etiquetas: [],
};

const EMPRESA_NO_PUBLICADA: Empresa = {
  ...EMPRESA_PUBLICADA,
  id: 2,
  nombre: 'Beta',
  publicada: false,
  observaciones: '',
  contactoNombre: '',
  contactoTelefono: '',
  contactoEmail: '',
  creadaPorCorreo: 'profesor@centro.es',
  fechaCreacion: '2026-01-01T00:00:00Z',
};

const EMPRESA_CON_ETIQUETA: Empresa = {
  ...EMPRESA_PUBLICADA,
  id: 3,
  nombre: 'Gamma',
  etiquetas: [{ id: 5, nombre: 'Diseño gráfico' }],
};

describe('listado de empresas', () => {
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

  async function loginComo(rol: 'ALUMNO' | 'PROFESOR'): Promise<void> {
    const promesa = auth.login('usuario@centro.es', 'secreta', '');
    http.expectOne('/api/auth/login').flush({ rol, esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  }

  it('sin sesión, /empresas redirige a login', async () => {
    auth.limpiarSesion();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    expect(router.url).toBe('/login');
  });

  it('alumno ve el listado sin el badge de publicación', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    // El backend real solo manda publicadas a un alumno — sin campo `publicada`.
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA_PUBLICADA]));
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Acme');
    // La vista alumno no trae `publicada`, así que nunca puede pintar la marca.
    expect(harness.routeNativeElement?.querySelector('.c-tarjeta-empresa__estado')).toBeNull();
  });

  it('profesor ve la marca de publicación de cada empresa', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    // Al profesor el backend le manda la vista completa de las dos, publicada o no.
    const publicada = { ...EMPRESA_NO_PUBLICADA, id: 4, nombre: 'Delta', publicada: true };
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([publicada, EMPRESA_NO_PUBLICADA]));
    await esperarMicrotareas();
    harness.detectChanges();

    const marcas = [...harness.routeNativeElement!.querySelectorAll('.c-tarjeta-empresa__estado')];
    expect(marcas.map((m) => m.getAttribute('aria-label'))).toEqual(['Publicada', 'Sin publicar']);
  });

  it('el filtro de la URL viaja al backend como parámetro', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas?publicada=false');
    const peticion = http.expectOne((r) => r.url === '/api/empresas');
    expect(peticion.request.params.get('publicada')).toBe('false');
    expect(peticion.request.params.get('tamano')).toBe('9');
    peticion.flush(pagina([EMPRESA_NO_PUBLICADA]));
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Beta');
  });

  it('el alumnado no ve las pastillas de filtro', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA_PUBLICADA]));
    await esperarMicrotareas();
    harness.detectChanges();

    // La lupa comparte pastilla con los filtros, así que se mira su lista.
    expect(harness.routeNativeElement?.querySelector('.o-etiquetas')).toBeNull();
  });

  it('la lupa se despliega y lo tecleado se consulta al backend', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA_PUBLICADA]));
    await esperarMicrotareas();
    harness.detectChanges();

    const lupa: HTMLButtonElement = harness.routeNativeElement!.querySelector('.c-buscador:not(.c-buscador--fijo) .c-buscador__lupa')!;
    expect(lupa.getAttribute('aria-expanded')).toBe('false');
    lupa.click();
    harness.detectChanges();
    expect(lupa.getAttribute('aria-expanded')).toBe('true');

    const entrada: HTMLInputElement = harness.routeNativeElement!.querySelector('.c-buscador:not(.c-buscador--fijo) .c-buscador__entrada')!;
    expect(document.activeElement).toBe(entrada);
    entrada.value = 'diseno';
    entrada.dispatchEvent(new Event('input'));
    // La consulta espera a que pare de teclear.
    await esperarTecleo();

    const busqueda = http.expectOne((r) => r.url === '/api/empresas');
    expect(busqueda.request.params.get('texto')).toBe('diseno');
    busqueda.flush(pagina([EMPRESA_CON_ETIQUETA]));
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Gamma');
    expect(router.url).toBe('/empresas?texto=diseno');
  });

  it('la paginación pide la página siguiente sin perder el filtro', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas?publicada=true');
    http
      .expectOne((r) => r.url === '/api/empresas')
      .flush(pagina([EMPRESA_PUBLICADA], { total: 12, paginas: 2 }));
    await esperarMicrotareas();
    harness.detectChanges();

    const siguiente = [...harness.routeNativeElement!.querySelectorAll('button')].find((b) =>
      b.textContent?.includes('Siguiente'),
    );
    siguiente?.click();
    await esperarMicrotareas();

    const segunda = http.expectOne((r) => r.url === '/api/empresas');
    expect(segunda.request.params.get('pagina')).toBe('1');
    expect(segunda.request.params.get('publicada')).toBe('true');
    segunda.flush(pagina([EMPRESA_CON_ETIQUETA], { pagina: 1, total: 12, paginas: 2 }));
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Página 2 de 2');
  });

  it('un fallo de red al listar muestra un mensaje de error legible', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas');
    http
      .expectOne((r) => r.url === '/api/empresas')
      .flush({ codigo: 'ERROR_INTERNO' }, { status: 500, statusText: 'Internal Server Error' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('No se pudieron cargar las empresas');
  });
});

describe('detalle de empresa', () => {
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

  async function loginComo(rol: 'ALUMNO' | 'PROFESOR'): Promise<void> {
    const promesa = auth.login('usuario@centro.es', 'secreta', '');
    http.expectOne('/api/auth/login').flush({ rol, esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  }

  it('alumno ve los datos comunes, sin editar ni gestión', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/1');
    http.expectOne('/api/empresas/1').flush(EMPRESA_PUBLICADA);
    await esperarMicrotareas();
    // La sesión post-login no trae id/correo (asimetría documentada) — se completa con /me.
    http
      .expectOne('/api/empresas/1/tasa-contratacion')
      .flush({ empresaId: 1, asignacionesDecididas: 4, contrataciones: 3, tasa: 0.75 });
    http.expectOne('/api/empresas/1/reviews').flush([]);
    http
      .expectOne('/api/auth/me')
      .flush({
        id: 10,
        correo: 'alumno@centro.es',
        rol: 'ALUMNO',
        esAdmin: false,
        debeCambiarContrasena: false,
        etiquetas: [],
      });
    await esperarMicrotareas();
    // Vista alumno: con la sesión ya completa, cruza sus propias asignaciones sin review
    // y consulta el estado de interés en esta empresa.
    http.expectOne('/api/alumnos/10/asignaciones').flush([]);
    http.expectOne('/api/alumnos/10/intereses').flush([]);
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Acme');
    expect(texto).not.toContain('Editar');
    expect(texto).not.toContain('Datos de gestión');
    // Tasa de contratación visible a cualquier rol, aquí como porcentaje (pipe percent, no cálculo manual).
    expect(texto).toContain('75%');
  });

  it('profesor ve editar, gestión y el badge de no publicada', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA_NO_PUBLICADA);
    await esperarMicrotareas();
    // Vista profesor: dispara además la carga de asignaciones, reviews e interesados
    // de la empresa, y completa la sesión con /me (post-login no trae id/correo).
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http.expectOne('/api/empresas/2/asignaciones').flush([]);
    http.expectOne('/api/empresas/2/reviews').flush([]);
    http.expectOne('/api/empresas/2/interesados').flush([]);
    http
      .expectOne('/api/auth/me')
      .flush({
        id: 5,
        correo: 'profesor@centro.es',
        rol: 'PROFESOR',
        esAdmin: false,
        debeCambiarContrasena: false,
        etiquetas: [],
      });
    await esperarMicrotareas();
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Editar');
    expect(texto).toContain('Datos de gestión');
    expect(texto).toContain('No publicada');
    // Sin ninguna asignación decidida: texto alternativo, nunca "0%" (induciría a error).
    expect(texto).toContain('Sin datos de contratación todavía');
    expect(texto).not.toContain('0%');
  });

  it('un 404 muestra un mensaje legible, no un error sin manejar', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/999');
    http
      .expectOne('/api/empresas/999')
      .flush({ codigo: 'EMPRESA_NO_ENCONTRADA' }, { status: 404, statusText: 'Not Found' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('no existe');
  });
});

describe('formulario de empresa', () => {
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

  async function loginComo(rol: 'ALUMNO' | 'PROFESOR'): Promise<void> {
    const promesa = auth.login('usuario@centro.es', 'secreta', '');
    http.expectOne('/api/auth/login').flush({ rol, esAdmin: false, debeCambiarContrasena: false });
    await promesa;
  }

  it('un alumno no puede acceder a /empresas/nueva', async () => {
    await loginComo('ALUMNO');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/nueva');
    // profesorGuard deniega y "/" redirige al listado.
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([]));
    await esperarMicrotareas();
    expect(router.url).toBe('/empresas');
  });

  it('profesor crea una empresa: etiquetas de checkbox + manuales se combinan sin duplicar', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl('/empresas/nueva', EmpresaFormularioPage);
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA_PUBLICADA])); // catálogo derivado del listado
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    c.form.patchValue({ nombre: 'Nueva SL', sectorId: 10, etiquetasManual: '20, 30, 20' });
    c.toggleEtiqueta(20, true);
    const envio = c.enviar() as Promise<void>;

    const creacion = http.expectOne((r) => r.url === '/api/empresas');
    expect(creacion.request.method).toBe('POST');
    expect(creacion.request.body).toMatchObject({
      nombre: 'Nueva SL',
      sectorId: 10,
      etiquetaIds: [20, 30],
      publicada: false,
    });
    creacion.flush({ ...EMPRESA_NO_PUBLICADA, id: 5, nombre: 'Nueva SL' });
    await envio;

    // La navegación a /empresas/5/editar monta una nueva instancia de la
    // página, que vuelve a pedir su propio catálogo y la empresa recién creada.
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA_NO_PUBLICADA]));
    await esperarMicrotareas();
    http.expectOne('/api/empresas/5').flush({ ...EMPRESA_NO_PUBLICADA, id: 5, nombre: 'Nueva SL' });
    await esperarMicrotareas();

    expect(router.url).toBe('/empresas/5/editar');
  });

  it('profesor edita una empresa existente y vuelve al detalle', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl('/empresas/2/editar', EmpresaFormularioPage);
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA_NO_PUBLICADA]));
    await esperarMicrotareas();
    http.expectOne('/api/empresas/2').flush(EMPRESA_NO_PUBLICADA);
    await esperarMicrotareas();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    expect(c.form.value.nombre).toBe('Beta');

    c.form.patchValue({ nombre: 'Beta renombrada', publicada: true });
    const envio = c.enviar() as Promise<void>;

    const edicion = http.expectOne('/api/empresas/2');
    expect(edicion.request.method).toBe('PUT');
    expect(edicion.request.body).toMatchObject({ nombre: 'Beta renombrada', publicada: true });
    edicion.flush({ ...EMPRESA_NO_PUBLICADA, nombre: 'Beta renombrada', publicada: true });
    await envio;

    // La navegación al detalle monta EmpresaDetallePage, que pide la empresa de nuevo.
    http.expectOne('/api/empresas/2').flush({ ...EMPRESA_NO_PUBLICADA, nombre: 'Beta renombrada' });
    await esperarMicrotareas();
    http
      .expectOne('/api/empresas/2/tasa-contratacion')
      .flush({ empresaId: 2, asignacionesDecididas: 0, contrataciones: 0, tasa: 0 });
    http.expectOne('/api/empresas/2/asignaciones').flush([]);
    http.expectOne('/api/empresas/2/reviews').flush([]);
    http.expectOne('/api/empresas/2/interesados').flush([]);
    http
      .expectOne('/api/auth/me')
      .flush({
        id: 5,
        correo: 'profesor@centro.es',
        rol: 'PROFESOR',
        esAdmin: false,
        debeCambiarContrasena: false,
        etiquetas: [],
      });
    await esperarMicrotareas();

    expect(router.url).toBe('/empresas/2');
  });

  it('profesor sube una imagen en la pantalla de editar', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl('/empresas/2/editar', EmpresaFormularioPage);
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA_NO_PUBLICADA]));
    await esperarMicrotareas();
    http.expectOne('/api/empresas/2').flush(EMPRESA_NO_PUBLICADA);
    await esperarMicrotareas();

    const fichero = new File(['contenido'], 'foto.jpg', { type: 'image/jpeg' });
    const evento = { target: { files: [fichero] } } as unknown as Event;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    const subida = c.onArchivoSeleccionado(evento) as Promise<void>;

    const peticion = http.expectOne('/api/empresas/2/imagen');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body instanceof FormData).toBe(true);
    peticion.flush({ ...EMPRESA_NO_PUBLICADA, imagen: '/uploads/empresas/foto.jpg' });
    await subida;

    expect(c.imagenActual()).toBe('/uploads/empresas/foto.jpg');
  });

  it('un fichero inválido deja un mensaje de error sin tocar la imagen actual', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    const componente = await harness.navigateByUrl('/empresas/2/editar', EmpresaFormularioPage);
    http.expectOne((r) => r.url === '/api/empresas').flush(pagina([EMPRESA_NO_PUBLICADA]));
    await esperarMicrotareas();
    http.expectOne('/api/empresas/2').flush(EMPRESA_NO_PUBLICADA);
    await esperarMicrotareas();

    const fichero = new File(['no-es-una-imagen'], 'foto.txt', { type: 'text/plain' });
    const evento = { target: { files: [fichero] } } as unknown as Event;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const c = componente as any;
    const subida = c.onArchivoSeleccionado(evento) as Promise<void>;

    http
      .expectOne('/api/empresas/2/imagen')
      .flush(
        { codigo: 'IMAGEN_INVALIDA', mensaje: 'El fichero no es una imagen válida' },
        { status: 400, statusText: 'Bad Request' },
      );
    await subida;

    expect(c.errorImagen()).toBe('El fichero no es una imagen válida');
    expect(c.imagenActual()).toBeNull();
  });
});
