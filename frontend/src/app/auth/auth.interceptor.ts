import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Cualquier 401 de la API significa sesión ausente/caducada: limpia el estado
 * y redirige a login. Se excluyen login (401 = credenciales inválidas) y
 * cambiar-contrasena (401 = contraseña actual incorrecta) — esos los maneja
 * su formulario.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const manejaSuPropio401 =
        req.url.includes('/api/auth/login') || req.url.includes('/api/auth/cambiar-contrasena');
      if (error.status === 401 && !manejaSuPropio401) {
        auth.limpiarSesion();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
