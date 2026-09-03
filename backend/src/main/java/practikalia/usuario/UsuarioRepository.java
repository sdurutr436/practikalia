package practikalia.usuario;

import java.time.Instant;
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

    /** Para el borrado del catálogo: ¿algún alumno tiene esta etiqueta entre sus intereses? */
    boolean existsByEtiquetasId(Long etiquetaId);

    long countByRolAndActivoTrue(Rol rol);

    /** Cuántos administradores quedan: el último no puede dejar de serlo. */
    long countByEsAdminTrue();

    /**
     * El profesorado del centro, para su pantalla. {@code conClase} filtra las
     * pastillas: {@code true} los que tutorizan una clase, {@code false} los que
     * no, {@code null} todos.
     */
    @Query("""
            select u from Usuario u
            where u.rol = :rol
              and (:conClase is null
                   or (:conClase = true and exists (select g.id from Grado g where g.tutor = u))
                   or (:conClase = false and not exists (select g.id from Grado g where g.tutor = u)))
            """)
    Page<Usuario> buscarProfesorado(Rol rol, Boolean conClase, Pageable pageable);

    /**
     * El alumnado de un curso académico, para la pantalla de asignaciones. Un
     * alumno pertenece al curso de su año de matrícula; si no lo tiene puesto
     * —es opcional tanto en el alta como en el CSV— al curso en el que se le dio
     * de alta, de ahí el rango sobre {@code fechaCreacion}. {@code asignado}
     * filtra las pastillas: {@code true} con asignación abierta, {@code false}
     * sin ninguna, {@code null} todos. {@code gradoId} y {@code texto} son los
     * otros dos filtros de la pantalla, ambos opcionales; el join a grado es
     * {@code left} para no dejar fuera al alumnado sin clase cuando no se
     * filtra por ella.
     *
     * ponytail: el texto es un LIKE en minúsculas sin quitar acentos, igual que
     * la búsqueda de empresas. Si hace falta que "ramirez" encuentre "Ramírez",
     * aquí entra un translate/unaccent — y en los dos sitios a la vez.
     */
    @Query("""
            select u from Usuario u left join u.grado g
            where u.rol = :rol
              and (u.anio = :curso
                   or (u.anio is null and u.fechaCreacion >= :inicio and u.fechaCreacion < :fin))
              and (:gradoId is null or g.id = :gradoId)
              and (:texto is null
                   or lower(u.nombre) like :texto
                   or lower(u.apellido1) like :texto
                   or lower(u.apellido2) like :texto
                   or lower(u.correo) like :texto)
              and (:asignado is null
                   or (:asignado = true and exists
                       (select a.id from Asignacion a where a.alumno = u and a.fechaFin is null))
                   or (:asignado = false and not exists
                       (select a.id from Asignacion a where a.alumno = u and a.fechaFin is null)))
            """)
    Page<Usuario> buscarDelCurso(Rol rol, int curso, Instant inicio, Instant fin,
            Long gradoId, String texto, Boolean asignado, Pageable pageable);

    /** Los cursos que tienen alumnado matriculado, para el selector de la pantalla de asignaciones. */
    @Query("select distinct u.anio from Usuario u where u.rol = :rol and u.anio is not null order by u.anio desc")
    List<Integer> cursosConAlumnado(Rol rol);

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
