package practikalia.review;

import practikalia.empresa.Empresa;
import practikalia.usuario.Usuario;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario alumno;

    @ManyToOne(optional = false)
    private Usuario autor;

    @ManyToOne(optional = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String contenido;

    @Column(nullable = false)
    private int calificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReview estado;

    @ManyToOne
    private Usuario moderadaPor;

    private String motivoRechazo;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion = Instant.now();

    private Instant fechaModeracion;

    public Review(Usuario alumno, Usuario autor, Empresa empresa, String contenido, int calificacion, EstadoReview estado) {
        this.alumno = alumno;
        this.autor = autor;
        this.empresa = empresa;
        this.contenido = contenido;
        this.calificacion = calificacion;
        this.estado = estado;
    }
}
