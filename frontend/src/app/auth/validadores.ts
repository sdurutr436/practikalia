import { AbstractControl, ValidationErrors } from '@angular/forms';

// Letra de control del DNI español: resto de la división entre 23 indexa esta
// cadena. Es una comprobación de formato (número + letra cuadran), no una
// verificación real de matriculación — esa la hace el centro al aprobar la
// cuenta pendiente.
const LETRAS_DNI = 'TRWAGMYFPDXBNJZSQVHLCKE';

export function dniValido(control: AbstractControl<string>): ValidationErrors | null {
  const coincide = /^(\d{8})([A-Za-z])$/.exec(control.value.trim());
  if (!coincide) {
    return { dni: true };
  }
  const [, numero, letra] = coincide;
  return LETRAS_DNI[Number(numero) % 23] === letra.toUpperCase() ? null : { dni: true };
}

// ponytail: solo se conoce el dominio general del centro (el mismo que ya usa
// el placeholder del login). Un dominio propio por institución necesita que
// el backend lo exponga primero — ver fase18_autoregistro_alumnos.md.
const CORREO_INSTITUCIONAL = /^[^\s@]+@g\.educaand\.es$/i;

export function correoInstitucional(control: AbstractControl<string>): ValidationErrors | null {
  return CORREO_INSTITUCIONAL.test(control.value) ? null : { correoInstitucional: true };
}
