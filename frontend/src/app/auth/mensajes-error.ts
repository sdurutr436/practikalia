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

/** Mensajes de UI por código del contrato de /api/asignaciones. */
export const MENSAJES_ASIGNACION: Record<string, string> = {
  ASIGNACION_NO_ENCONTRADA: 'La asignación no existe.',
  ASIGNACION_YA_EXISTE: 'Ya existe una asignación de ese alumno a esa empresa para ese grado y año.',
  ALUMNO_INVALIDO: 'El usuario seleccionado como alumno no tiene rol de alumno.',
  TUTOR_INVALIDO: 'El usuario seleccionado como tutor no tiene rol de profesor.',
  ALUMNO_NO_ENCONTRADO: 'El alumno seleccionado no existe.',
  TUTOR_NO_ENCONTRADO: 'El tutor seleccionado no existe.',
  EMPRESA_NO_ENCONTRADA: 'La empresa no existe.',
  GRADO_NO_ENCONTRADO: 'El grado seleccionado no existe.',
};

/** Mensajes de UI por código del contrato de PUT /api/usuarios/{id}/grado. */
export const MENSAJES_GRADO: Record<string, string> = {
  USUARIO_NO_ENCONTRADO: 'El alumno no existe.',
  GRADO_NO_ENCONTRADO: 'El grado seleccionado no existe.',
  CAMPO_INVALIDO: 'Selecciona un grado y un año válidos.',
};

/** Mensajes de UI por código del contrato de /api/reviews. */
export const MENSAJES_REVIEW: Record<string, string> = {
  ACCESO_DENEGADO: 'No eres el alumno ni el tutor de esa asignación.',
  CAMPO_INVALIDO: 'La calificación está fuera del rango permitido.',
  ASIGNACION_NO_ENCONTRADA: 'La asignación indicada no existe.',
  REVIEW_YA_EXISTE: 'Ya existe una review para esta asignación.',
  REVIEW_NO_ENCONTRADA: 'La review no existe.',
  REVIEW_YA_MODERADA: 'Esta review ya ha sido moderada.',
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
