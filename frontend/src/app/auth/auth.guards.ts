import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Ruta normal: exige sesión; con cambio de contraseña pendiente redirige de
 * forma bloqueante a esa pantalla.
 */
export const autenticadoGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const sesion = await auth.asegurarSesion();
  if (!sesion) {
    return router.createUrlTree(['/login']);
  }
  if (sesion.debeCambiarContrasena) {
    return router.createUrlTree(['/cambiar-contrasena']);
  }
  return true;
};

/**
 * Pantalla de cambio de contraseña: solo accesible con el cambio pendiente;
 * con sesión normal se vuelve a la ruta por defecto.
 */
export const cambioContrasenaPendienteGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const sesion = await auth.asegurarSesion();
  if (!sesion) {
    return router.createUrlTree(['/login']);
  }
  if (!sesion.debeCambiarContrasena) {
    return router.createUrlTree(['/']);
  }
  return true;
};
