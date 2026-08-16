import { HttpErrorResponse } from '@angular/common/http';
import { MENSAJES_LOGIN, mensajeDeError } from './mensajes-error';

function errorHttp(status: number, body: unknown): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: body });
}

describe('mensajeDeError', () => {
  it('mapea un código conocido del contrato a su mensaje de UI', () => {
    const error = errorHttp(401, { codigo: 'CREDENCIALES_INVALIDAS', mensaje: 'otro texto' });
    expect(mensajeDeError(error, MENSAJES_LOGIN)).toBe('Correo o contraseña incorrectos.');
  });

  it('con código desconocido usa el mensaje del backend', () => {
    const error = errorHttp(500, { codigo: 'ERROR_INTERNO', mensaje: 'Error interno' });
    expect(mensajeDeError(error, MENSAJES_LOGIN)).toBe('Error interno');
  });

  it('sin cuerpo reconocible cae al mensaje genérico', () => {
    expect(mensajeDeError(errorHttp(0, null), MENSAJES_LOGIN)).toContain('No se pudo');
    expect(mensajeDeError(new Error('x'), MENSAJES_LOGIN)).toContain('No se pudo');
  });
});
