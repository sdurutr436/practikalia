import { FormControl } from '@angular/forms';
import { correoInstitucional, dniValido } from './login-page';

describe('dniValido', () => {
  const validar = (valor: string) => dniValido(new FormControl(valor, { nonNullable: true }));

  it('acepta un DNI cuya letra de control cuadra', () => {
    expect(validar('12345678Z')).toBeNull();
    expect(validar('00000000T')).toBeNull();
    expect(validar('12345678z')).toBeNull(); // minúscula
  });

  it('rechaza letra de control incorrecta o formato inválido', () => {
    expect(validar('12345678A')).toEqual({ dni: true }); // letra que no cuadra
    expect(validar('1234567Z')).toEqual({ dni: true }); // menos de 8 dígitos
    expect(validar('1234567AZ')).toEqual({ dni: true }); // letra en el número
  });
});

describe('correoInstitucional', () => {
  const validar = (valor: string) =>
    correoInstitucional(new FormControl(valor, { nonNullable: true }));

  it('acepta el dominio general del centro', () => {
    expect(validar('alumno@g.educaand.es')).toBeNull();
  });

  it('rechaza un correo personal', () => {
    expect(validar('alumno@gmail.com')).toEqual({ correoInstitucional: true });
  });
});
