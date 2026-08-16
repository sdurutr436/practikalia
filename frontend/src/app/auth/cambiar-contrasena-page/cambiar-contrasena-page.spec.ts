import { FormControl } from '@angular/forms';
import { politicaContrasena } from './cambiar-contrasena-page';

describe('politicaContrasena', () => {
  const validar = (valor: string) => politicaContrasena(new FormControl(valor, { nonNullable: true }));

  it('acepta una contraseña que cumple la política del backend', () => {
    expect(validar('Nueva.1234')).toBeNull();
  });

  it('rechaza las que incumplen cada requisito', () => {
    expect(validar('Corta.1')).toEqual({ politica: true }); // menos de 8
    expect(validar('minuscula.123')).toEqual({ politica: true }); // sin mayúscula
    expect(validar('MAYUSCULA.123')).toEqual({ politica: true }); // sin minúscula
    expect(validar('SinNumeros.!')).toEqual({ politica: true }); // sin dígito
    expect(validar('SinEspecial123')).toEqual({ politica: true }); // sin especial
  });
});
