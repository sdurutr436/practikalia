package practikalia.asignacion;

import practikalia.empresa.Empresa;
import practikalia.grado.Grado;
import practikalia.usuario.Usuario;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario alumno;

    @ManyToOne(optional = false)
    private Empresa empresa;

    @ManyToOne(optional = false)
    private Usuario tutorCentro;

    @ManyToOne(optional = false)
    private Grado grado;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private Boolean contratadoPosterior;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion = Instant.now();

    public Asignacion(Usuario alumno, Empresa empresa, Usuario tutorCentro, Grado grado, int anio, LocalDate fechaInicio) {
        this.alumno = alumno;
        this.empresa = empresa;
        this.tutorCentro = tutorCentro;
        this.grado = grado;
        this.anio = anio;
        this.fechaInicio = fechaInicio;
    }
}
