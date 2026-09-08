import { HttpErrorResponse } from '@angular/common/http';

/** Mensajes de UI por código del contrato de /api/auth. */
export const MENSAJES_LOGIN: Record<string, string> = {
  CREDENCIALES_INVALIDAS: 'Correo o contraseña incorrectos.',
  CUENTA_NO_DISPONIBLE: 'La cuenta no está disponible. Contacta con el centro.',
  DEMASIADOS_INTENTOS: 'Demasiados intentos fallidos. Espera 15 minutos y vuelve a intentarlo.',
};

/** Mensajes de UI por código del contrato de POST /api/auth/registro. */
export const MENSAJES_REGISTRO: Record<string, string> = {
  CORREO_YA_EXISTE: 'Ya hay una cuenta con ese correo.',
  CORREO_DOMINIO_NO_PERMITIDO: 'Usa tu correo institucional del centro.',
  DNI_INVALIDO: 'El DNI no es válido: revisa el número y la letra.',
  GRADO_NO_ENCONTRADO: 'Selecciona una clase de la lista.',
};

export const MENSAJES_CAMBIO_CONTRASENA: Record<string, string> = {
  CONTRASENA_ACTUAL_INCORRECTA: 'La contraseña actual no es correcta.',
  CONTRASENA_NO_CUMPLE_POLITICA:
    'La contraseña nueva no cumple la política: mínimo 8 caracteres con mayúscula, minúscula, número y carácter especial.',
};

/**
 * Códigos que comparten el alta y la edición de cualquier cuenta del centro:
 * los usan el listado de alumnado y el de profesorado, cada uno con los suyos
 * propios encima.
 */
export const MENSAJES_CUENTA: Record<string, string> = {
  CORREO_YA_EXISTE: 'Ya hay otra cuenta con ese correo.',
  DNI_INVALIDO: 'El DNI no es válido: revisa el número y la letra.',
  DNI_YA_REGISTRADO: 'Ya hay una cuenta con ese DNI.',
  CORREO_DOMINIO_NO_PERMITIDO: 'Ese correo no es de un dominio que admita el centro.',
  GRADO_NO_ENCONTRADO: 'La clase seleccionada ya no existe.',
};

/** Mensajes de UI por código del contrato de /api/empresas (crear/editar/imagen). */
export const MENSAJES_EMPRESA: Record<string, string> = {
  ETIQUETA_NO_ENCONTRADA: 'El sector o alguna de las etiquetas indicadas no existe.',
  EMPRESA_NO_ENCONTRADA: 'La empresa no existe.',
};

/** Mensajes de UI por código del contrato de /api/asignaciones. */
export const MENSAJES_ASIGNACION: Record<string, string> = {
  ASIGNACION_NO_ENCONTRADA: 'La asignación no existe.',
  ASIGNACION_YA_EXISTE:
    'Ya existe una asignación de ese alumno a esa empresa para ese grado y año.',
  ALUMNO_INVALIDO: 'El usuario seleccionado como alumno no tiene rol de alumno.',
  TUTOR_INVALIDO: 'El usuario seleccionado como tutor no tiene rol de profesor.',
  ALUMNO_NO_ENCONTRADO: 'El alumno seleccionado no existe.',
  TUTOR_NO_ENCONTRADO: 'El tutor seleccionado no existe.',
  EMPRESA_NO_ENCONTRADA: 'La empresa no existe.',
  GRADO_NO_ENCONTRADO: 'El grado seleccionado no existe.',
  EMPRESA_NO_PUBLICADA: 'Esa empresa todavía no está confirmada.',
  ALUMNO_SIN_CLASE: 'Ponle clase al alumno en Alumnado antes de asignarle empresa.',
};

/** Mensajes de UI por código del contrato de mantenimiento de /api/etiquetas. */
export const MENSAJES_CATALOGO: Record<string, string> = {
  ETIQUETA_REPETIDA: 'Ya hay un sector o una etiqueta con ese nombre.',
  ETIQUETA_CON_HIJAS: 'Todavía cuelgan actividades o etiquetas de aquí; vacíalo antes de borrarlo.',
  ETIQUETA_EN_USO: 'No se puede borrar: alguna empresa o algún alumno la está usando.',
  ETIQUETA_NO_ENCONTRADA: 'Ese sector o esa etiqueta ya no existe.',
  NIVEL_MAXIMO: 'El catálogo llega hasta la etiqueta: sector, actividad y etiqueta.',
  CAMPO_INVALIDO: 'Escribe un nombre.',
};

/** Mensajes de UI por código del contrato de PUT /api/usuarios/{id}/grado. */
export const MENSAJES_GRADO: Record<string, string> = {
  USUARIO_NO_ENCONTRADO: 'El alumno no existe.',
  GRADO_NO_ENCONTRADO: 'El grado seleccionado no existe.',
  CAMPO_INVALIDO: 'Selecciona un grado y un año válidos.',
};

/** Mensajes de UI por código del contrato de /api/empresas/{id}/interes. */
export const MENSAJES_INTERES: Record<string, string> = {
  ALUMNO_SIN_GRADO: 'Primero necesitas que un profesor te asigne un grado.',
  EMPRESA_NO_ENCONTRADA: 'La empresa no existe, o no está publicada.',
};

/** Mensajes de UI por código del contrato de PUT /api/usuarios/{id}/etiquetas. */
export const MENSAJES_PERFIL: Record<string, string> = {
  ETIQUETA_NO_ENCONTRADA: 'Alguna de las etiquetas seleccionadas ya no existe.',
  USUARIO_NO_ENCONTRADO: 'Tu usuario no existe.',
  ACCESO_DENEGADO: 'No tienes permiso para editar estas etiquetas.',
};

/** Mensajes de UI por código del contrato de /api/alumnos/{id}/afinidad. */
export const MENSAJES_AFINIDAD: Record<string, string> = {
  ACCESO_DENEGADO: 'No eres tutor de ninguna asignación activa de este alumno.',
  USUARIO_NO_ENCONTRADO: 'El alumno no existe.',
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

/** Mensajes de UI por código del contrato de POST /api/usuarios. */
export const MENSAJES_ALTA_ALUMNO: Record<string, string> = {
  CORREO_NO_PERMITIDO: 'Ese correo no está permitido en este centro.',
  CORREO_YA_REGISTRADO: 'Ya hay una cuenta con ese correo.',
  ACCESO_DENEGADO: 'No tienes permiso para dar de alta cuentas.',
};

/** Mensajes de UI por código del contrato de /api/centro y /api/correos-permitidos. */
export const MENSAJES_CENTRO: Record<string, string> = {
  CORREO_PERMITIDO_YA_EXISTE: 'Ese correo ya está en la whitelist.',
  CORREO_PERMITIDO_NO_ENCONTRADO: 'Ese correo ya no está en la whitelist.',
  IMAGEN_INVALIDA: 'La imagen no es válida: usa JPEG, PNG o WebP de menos de 5 MB.',
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
