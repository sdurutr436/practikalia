package practikalia.interes;

import practikalia.empresa.Empresa;
import practikalia.grado.Grado;
import practikalia.usuario.Usuario;

import java.time.Instant;

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
public class Interes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario alumno;

    @ManyToOne(optional = false)
    private Empresa empresa;

    @ManyToOne(optional = false)
    private Grado grado;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion = Instant.now();

    public Interes(Usuario alumno, Empresa empresa, Grado grado, int anio) {
        this.alumno = alumno;
        this.empresa = empresa;
        this.grado = grado;
        this.anio = anio;
    }
}
