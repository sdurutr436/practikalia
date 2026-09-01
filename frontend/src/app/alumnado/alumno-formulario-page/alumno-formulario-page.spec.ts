import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AlumnoFormularioPage } from './alumno-formulario-page';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

describe('alta de alumno', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function pintar() {
    const fixture = TestBed.createComponent(AlumnoFormularioPage);
    fixture.detectChanges();
    return fixture;
  }

  const escribirCorreo = (fixture: { nativeElement: HTMLElement }, correo: string) => {
    const entrada: HTMLInputElement = fixture.nativeElement.querySelector('#correo')!;
    entrada.value = correo;
    entrada.dispatchEvent(new Event('input'));
  };

  const enviar = (fixture: { nativeElement: HTMLElement }) =>
    fixture.nativeElement.querySelector('form')?.dispatchEvent(new Event('submit'));

  it('crea el alumno y enseña la contraseña temporal', async () => {
    const fixture = pintar();
    escribirCorreo(fixture, 'lucia@centro.es');
    fixture.detectChanges();
    enviar(fixture);

    const peticion = http.expectOne('/api/usuarios');
    expect(peticion.request.body).toEqual({ correo: 'lucia@centro.es', rol: 'ALUMNO' });
    peticion.flush({
      id: 9,
      correo: 'lucia@centro.es',
      rol: 'ALUMNO',
      contrasenaTemporal: 'Temporal123!',
    });
    await esperarMicrotareas();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Temporal123!');
  });

  it('un correo fuera de la whitelist se explica sin tecnicismos', async () => {
    const fixture = pintar();
    escribirCorreo(fixture, 'ajeno@otrositio.es');
    fixture.detectChanges();
    enviar(fixture);

    http
      .expectOne('/api/usuarios')
      .flush({ codigo: 'CORREO_NO_PERMITIDO' }, { status: 403, statusText: 'Forbidden' });
    await esperarMicrotareas();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('no está permitido en este centro');
  });

  it('sin correo válido no llama al backend', async () => {
    const fixture = pintar();
    enviar(fixture);
    await esperarMicrotareas();

    // http.verify() del afterEach falla si se hubiera llamado a /api/usuarios.
    expect(fixture.nativeElement.querySelector('#correo')).not.toBeNull();
  });
});
