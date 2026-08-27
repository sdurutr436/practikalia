import { HttpErrorResponse } from '@angular/common/http';

/** Mensajes de UI por código del contrato de /api/auth. */
export const MENSAJES_LOGIN: Record<string, string> = {
  CREDENCIALES_INVALIDAS: 'Correo o contraseña incorrectos.',
  CUENTA_NO_DISPONIBLE: 'La cuenta no está disponible. Contacta con el centro.',
  DEMASIADOS_INTENTOS: 'Demasiados intentos fallidos. Espera 15 minutos y vuelve a intentarlo.',
};

export const MENSAJES_CAMBIO_CONTRASENA: Record<string, string> = {
  CONTRASENA_ACTUAL_INCORRECTA: 'La contraseña actual no es correcta.',
  CONTRASENA_NO_CUMPLE_POLITICA:
    'La contraseña nueva no cumple la política: mínimo 8 caracteres con mayúscula, minúscula, número y carácter especial.',
};

/** Mensajes de UI por código del contrato de /api/empresas (crear/editar/imagen). */
export const MENSAJES_EMPRESA: Record<string, string> = {
  ETIQUETA_NO_ENCONTRADA: 'El sector o alguna de las etiquetas indicadas no existe.',
  EMPRESA_NO_ENCONTRADA: 'La empresa no existe.',
};

/** Código conocido → su mensaje; si no, el mensaje del backend; si no, genérico. */
export function mensajeDeError(error: unknown, mensajes: Record<string, string>): string {
  if (error instanceof HttpErrorResponse) {
    const codigo: unknown = error.error?.codigo;
    if (typeof codigo === 'string' && mensajes[codigo]) {
      return mensajes[codigo];
    }
    if (typeof error.error?.mensaje === 'string') {
      return error.error.mensaje;
    }
  }
  return 'No se pudo completar la operación. Inténtalo de nuevo.';
}
