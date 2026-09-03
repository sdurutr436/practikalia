package practikalia.grado;

import practikalia.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    /**
     * El profesor que la tutoriza, o {@code null} si todavía no tiene ninguno.
     * Uno como mucho, y el mismo profesor no puede tutorizar dos clases: la
     * columna es única.
     */
    @OneToOne
    @JoinColumn(name = "tutor_id", unique = true)
    private Usuario tutor;

    public Grado(String nombre) {
        this.nombre = nombre;
    }
}
