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

/**
 * Rutas de escritura (crear/editar empresa, subir imagen, etc.): solo
 * profesor/admin (`esAdmin` no es un rol aparte, siempre viaja sobre
 * `rol=PROFESOR`). Chequeo de UI — la barrera real la pone el backend.
 */
export const profesorGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const sesion = await auth.asegurarSesion();
  if (!sesion) {
    return router.createUrlTree(['/login']);
  }
  if (sesion.debeCambiarContrasena) {
    return router.createUrlTree(['/cambiar-contrasena']);
  }
  if (sesion.rol !== 'PROFESOR') {
    return router.createUrlTree(['/']);
  }
  return true;
};

/**
 * Rutas de administración del centro (catálogo de sectores y etiquetas): hace
 * falta `esAdmin`, que viaja sobre `rol=PROFESOR` y no es un rol aparte.
 * Chequeo de UI — la barrera real la pone el backend.
 */
export const adminGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const sesion = await auth.asegurarSesion();
  if (!sesion) {
    return router.createUrlTree(['/login']);
  }
  if (sesion.debeCambiarContrasena) {
    return router.createUrlTree(['/cambiar-contrasena']);
  }
  if (!sesion.esAdmin) {
    return router.createUrlTree(['/']);
  }
  return true;
};

/** Ruta de "mis intereses": solo alumno, simétrico a profesorGuard. */
export const alumnoGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const sesion = await auth.asegurarSesion();
  if (!sesion) {
    return router.createUrlTree(['/login']);
  }
  if (sesion.debeCambiarContrasena) {
    return router.createUrlTree(['/cambiar-contrasena']);
  }
  if (sesion.rol !== 'ALUMNO') {
    return router.createUrlTree(['/']);
  }
  return true;
};
