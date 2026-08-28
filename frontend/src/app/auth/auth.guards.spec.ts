import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateFn, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { autenticadoGuard, cambioContrasenaPendienteGuard, profesorGuard } from './auth.guards';
import { AuthService, Sesion } from './auth.service';

function configurar(sesion: Sesion | null): void {
  TestBed.configureTestingModule({
    providers: [
      provideRouter([]),
      { provide: AuthService, useValue: { asegurarSesion: () => Promise.resolve(sesion) } },
    ],
  });
}

function ejecutar(guard: CanActivateFn): Promise<boolean | UrlTree> {
  return TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  ) as Promise<boolean | UrlTree>;
}

const sesionNormal: Sesion = {
  id: 1,
  correo: 'a@b.es',
  rol: 'ALUMNO',
  esAdmin: false,
  debeCambiarContrasena: false,
};

describe('autenticadoGuard', () => {
  it('sin sesión redirige a login', async () => {
    configurar(null);
    expect(String(await ejecutar(autenticadoGuard))).toBe('/login');
  });

  it('con cambio de contraseña pendiente redirige a esa pantalla', async () => {
    configurar({ ...sesionNormal, debeCambiarContrasena: true });
    expect(String(await ejecutar(autenticadoGuard))).toBe('/cambiar-contrasena');
  });

  it('con sesión normal deja pasar', async () => {
    configurar(sesionNormal);
    expect(await ejecutar(autenticadoGuard)).toBe(true);
  });
});

describe('cambioContrasenaPendienteGuard', () => {
  it('sin sesión redirige a login', async () => {
    configurar(null);
    expect(String(await ejecutar(cambioContrasenaPendienteGuard))).toBe('/login');
  });

  it('con sesión normal vuelve a la ruta por defecto', async () => {
    configurar(sesionNormal);
    expect(String(await ejecutar(cambioContrasenaPendienteGuard))).toBe('/');
  });

  it('con cambio pendiente deja pasar', async () => {
    configurar({ ...sesionNormal, debeCambiarContrasena: true });
    expect(await ejecutar(cambioContrasenaPendienteGuard)).toBe(true);
  });
});

describe('profesorGuard', () => {
  it('sin sesión redirige a login', async () => {
    configurar(null);
    expect(String(await ejecutar(profesorGuard))).toBe('/login');
  });

  it('con cambio de contraseña pendiente redirige a esa pantalla', async () => {
    configurar({ ...sesionNormal, rol: 'PROFESOR', debeCambiarContrasena: true });
    expect(String(await ejecutar(profesorGuard))).toBe('/cambiar-contrasena');
  });

  it('alumno no pasa, vuelve a la ruta por defecto', async () => {
    configurar(sesionNormal); // rol ALUMNO
    expect(String(await ejecutar(profesorGuard))).toBe('/');
  });

  it('profesor pasa', async () => {
    configurar({ ...sesionNormal, rol: 'PROFESOR' });
    expect(await ejecutar(profesorGuard)).toBe(true);
  });
});
