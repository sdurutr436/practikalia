import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { AuthService } from '../auth/auth.service';
import { authInterceptor } from '../auth/auth.interceptor';
import { Empresa } from '../empresas/empresa.model';
import { Asignacion } from './asignacion.model';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

const EMPRESA_PROFESOR: Empresa = {
  id: 2,
  nombre: 'Beta',
  descripcion: 'Una empresa',
  imagen: null,
  direccion: 'Calle Falsa 123',
  sector: { id: 10, nombre: 'Informática' },
  etiquetas: [],
  publicada: true,
  observaciones: '',
  contactoNombre: '',
  contactoTelefono: '',
  contactoEmail: '',
  creadaPorCorreo: 'profesor@centro.es',
  fechaCreacion: '2026-01-01T00:00:00Z',
};

const ASIGNACION_ABIERTA: Asignacion = {
  id: 5,
  alumnoId: 10,
  alumnoCorreo: 'alumno@centro.es',
  empresaId: 2,
  empresaNombre: 'Beta',
  tutorCentroId: 30,
  tutorCentroCorreo: 'profesor@centro.es',
  grado: { id: 40, nombre: 'DAM' },
  anio: 2026,
  fechaInicio: '2026-09-01',
  fechaFin: null,
  contratadoPosterior: null,
};

describe('sección de asignaciones en el detalle de empresa', () => {
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

  it('profesor ve el histórico de asignaciones y puede cerrar una inline', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA_PROFESOR);
    await esperarMicrotareas();
    http.expectOne('/api/empresas/2/asignaciones').flush([ASIGNACION_ABIERTA]);
    await esperarMicrotareas();
    harness.detectChanges();

    let texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('alumno@centro.es');
    expect(texto).toContain('DAM');
    expect(texto).toContain('abierta');

    const contenedor = harness.routeNativeElement?.querySelector('.c-ficha-empresa__gestion:last-of-type') as HTMLElement;
    const inputFecha = contenedor.querySelector('input[type="date"]') as HTMLInputElement;
    const select = contenedor.querySelector('select') as HTMLSelectElement;
    const boton = contenedor.querySelector('button.c-boton') as HTMLButtonElement;
    inputFecha.value = '2027-06-30';
    select.value = 'true';
    boton.click();

    const peticion = http.expectOne('/api/asignaciones/5');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ fechaFin: '2027-06-30', contratadoPosterior: true });
    peticion.flush({ ...ASIGNACION_ABIERTA, fechaFin: '2027-06-30', contratadoPosterior: true });
    await esperarMicrotareas();
    harness.detectChanges();

    texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('2027-06-30');
    expect(texto).toContain('sí');
  });

  it('un 404 al cerrar deja un mensaje de error legible en la fila', async () => {
    await loginComo('PROFESOR');
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/empresas/2');
    http.expectOne('/api/empresas/2').flush(EMPRESA_PROFESOR);
    await esperarMicrotareas();
    http.expectOne('/api/empresas/2/asignaciones').flush([ASIGNACION_ABIERTA]);
    await esperarMicrotareas();
    harness.detectChanges();

    const contenedor = harness.routeNativeElement?.querySelector('.c-ficha-empresa__gestion:last-of-type') as HTMLElement;
    const inputFecha = contenedor.querySelector('input[type="date"]') as HTMLInputElement;
    const boton = contenedor.querySelector('button.c-boton') as HTMLButtonElement;
    inputFecha.value = '2027-06-30';
    boton.click();

    http
      .expectOne('/api/asignaciones/5')
      .flush({ codigo: 'ASIGNACION_NO_ENCONTRADA' }, { status: 404, statusText: 'Not Found' });
    await esperarMicrotareas();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('La asignación no existe');
  });
});
