/** Lo que hace falta para escribir el nombre de alguien: alumno, profesor o tutor. */
export interface ConNombre {
  nombre: string | null;
  apellido1: string | null;
  apellido2: string | null;
}

/**
 * Nombre y apellidos de una persona. El respaldo es lo que se pinta mientras la
 * ficha no tiene nombre: las cuentas dadas de alta desde `POST /api/usuarios`
 * nacen sin él.
 */
export function nombreCompleto(persona: ConNombre, respaldo = 'Sin nombre'): string {
  const partes = [persona.nombre, persona.apellido1, persona.apellido2].filter(Boolean);
  return partes.length > 0 ? partes.join(' ') : respaldo;
}
