package practikalia.review;

import practikalia.asignacion.Asignacion;
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
    private Asignacion asignacion;

    @ManyToOne(optional = false)
    private Usuario autor;

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

    public Review(Asignacion asignacion, Usuario autor, String contenido, int calificacion, EstadoReview estado) {
        this.asignacion = asignacion;
        this.autor = autor;
        this.contenido = contenido;
        this.calificacion = calificacion;
        this.estado = estado;
    }
}
