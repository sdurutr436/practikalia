package practikalia.usuario;

/** Rol base de un usuario. La condición de administrador es un flag aparte ({@code Usuario.esAdmin}), no un valor de este enum. */
public enum Rol {
    ALUMNO,
    PROFESOR
}
