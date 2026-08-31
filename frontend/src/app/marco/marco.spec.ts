import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { MarcoComponent } from './marco';
import { AuthService, Sesion } from '../auth/auth.service';

const esperarMicrotareas = () => new Promise((resolve) => setTimeout(resolve));

describe('marco de la aplicación', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'login', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  function pintar(rol: 'ALUMNO' | 'PROFESOR') {
    const sesion: Sesion = {
      id: 1,
      correo: 'quien@centro.es',
      rol,
      esAdmin: false,
      debeCambiarContrasena: false,
    };
    auth.sesion.set(sesion);
    const fixture = TestBed.createComponent(MarcoComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('el menú del alumnado lleva a sus pantallas, no a las del profesorado', () => {
    const texto = pintar('ALUMNO').nativeElement.textContent ?? '';
    expect(texto).toContain('Mis intereses');
    expect(texto).toContain('Mis etiquetas');
    expect(texto).toContain('Mi afinidad');
    expect(texto).not.toContain('Nueva empresa');
    expect(texto).not.toContain('Asignaciones');
  });

  it('el menú del profesorado lleva a reseñas y a crear empresa', () => {
    const texto = pintar('PROFESOR').nativeElement.textContent ?? '';
    expect(texto).toContain('Reseñas');
    expect(texto).toContain('Asignaciones');
    expect(texto).toContain('Nueva empresa');
    expect(texto).not.toContain('Mis intereses');
  });

  it('el acordeón del menú se abre y se cierra desde la hamburguesa', () => {
    const fixture = pintar('ALUMNO');
    const menu: HTMLElement = fixture.nativeElement.querySelector('#menu-principal');
    const hamburguesa: HTMLButtonElement =
      fixture.nativeElement.querySelector('.c-barra__alternar');

    expect(menu.hidden).toBe(false);
    expect(hamburguesa.getAttribute('aria-expanded')).toBe('true');
    expect(hamburguesa.getAttribute('aria-controls')).toBe('menu-principal');

    hamburguesa.click();
    fixture.detectChanges();

    expect(menu.hidden).toBe(true);
    expect(hamburguesa.getAttribute('aria-expanded')).toBe('false');
  });

  it('cerrar sesión desde el marco limpia la sesión y vuelve a login', async () => {
    const fixture = pintar('ALUMNO');

    const boton = [...fixture.nativeElement.querySelectorAll('button')].find(
      (b: HTMLButtonElement) => b.textContent?.includes('Cerrar sesión'),
    );
    boton?.dispatchEvent(new Event('click'));
    http.expectOne('/api/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
    await esperarMicrotareas();

    expect(auth.sesion()).toBeNull();
    expect(router.url).toBe('/login');
  });
});
