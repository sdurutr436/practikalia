package practikalia.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    List<Usuario> findByRol(Rol rol);

    Page<Usuario> findByRol(Rol rol, Pageable pageable);

    Page<Usuario> findByRolAndActivo(Rol rol, boolean activo, Pageable pageable);

    boolean existsByDni(String dni);

    long countByRolAndActivoTrue(Rol rol);

    /**
     * Cuenta los usuarios activos de ese rol sin ninguna asignación abierta.
     * "Sin asignar" mira solo las abiertas ({@code fechaFin} nula): un alumno
     * cuya única asignación ya se cerró vuelve a estar libre para otra, así que
     * cuenta.
     */
    @Query("""
            select count(u) from Usuario u
            where u.rol = :rol and u.activo = true
              and not exists (select a.id from Asignacion a where a.alumno = u and a.fechaFin is null)
            """)
    long countActivosSinAsignacionAbierta(Rol rol);
}
