import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { catchError, firstValueFrom, throwError } from 'rxjs';

/** Estado de sesión en memoria; la cookie httpOnly es la sesión real. */
export interface Sesion {
  /** Solo lo devuelve GET /me; tras un login es null hasta rehidratar. */
  correo: string | null;
  rol: string;
  esAdmin: boolean;
  debeCambiarContrasena: boolean;
}

interface LoginResponse {
  rol: string;
  esAdmin: boolean;
  debeCambiarContrasena: boolean;
}

interface MeResponse extends LoginResponse {
  correo: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** null = sin sesión conocida. Se rehidrata con GET /me la primera vez que un guard lo pide. */
  readonly sesion = signal<Sesion | null>(null);
  private rehidratada = false;

  async login(correo: string, contrasena: string, web: string): Promise<Sesion> {
    const body = { correo, contrasena, web };
    const respuesta = await firstValueFrom(
      this.http.post<LoginResponse>('/api/auth/login', body).pipe(
        // ponytail: la cookie XSRF-TOKEN se escribe perezosa en el backend — el
        // primer POST de la app puede salir sin token y caer en el 403 genérico
        // de CSRF, cuya propia respuesta ya fija la cookie. Un único reintento
        // basta; los 403 de negocio (CUENTA_NO_DISPONIBLE) no entran aquí.
        catchError((error: HttpErrorResponse) =>
          error.status === 403 && error.error?.codigo === 'ACCESO_DENEGADO'
            ? this.http.post<LoginResponse>('/api/auth/login', body)
            : throwError(() => error),
        ),
      ),
    );
    const sesion: Sesion = { correo: null, ...respuesta };
    this.sesion.set(sesion);
    this.rehidratada = true;
    return sesion;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/logout', null));
    } finally {
      this.limpiarSesion();
    }
  }

  async me(): Promise<Sesion> {
    const respuesta = await firstValueFrom(this.http.get<MeResponse>('/api/auth/me'));
    this.sesion.set(respuesta);
    this.rehidratada = true;
    return respuesta;
  }

  async cambiarContrasena(contrasenaActual: string, contrasenaNueva: string): Promise<void> {
    await firstValueFrom(
      this.http.post<void>('/api/auth/cambiar-contrasena', { contrasenaActual, contrasenaNueva }),
    );
    const actual = this.sesion();
    if (actual) {
      this.sesion.set({ ...actual, debeCambiarContrasena: false });
    }
  }

  /**
   * Devuelve la sesión, rehidratándola una sola vez desde GET /me si la app
   * acaba de arrancar. Un fallo (401 sin cookie; 403 con el token restringido
   * de cambio de contraseña, que no autoriza /me) se trata como "sin sesión".
   */
  async asegurarSesion(): Promise<Sesion | null> {
    if (this.rehidratada) {
      return this.sesion();
    }
    try {
      return await this.me();
    } catch {
      this.limpiarSesion();
      return null;
    }
  }

  limpiarSesion(): void {
    this.sesion.set(null);
    this.rehidratada = true;
  }
}
