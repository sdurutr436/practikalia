package practikalia.etiqueta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nodo del catálogo. La misma entidad hace de sector, de actividad principal y
 * de etiqueta suelta: lo que la distingue es de quién cuelga.
 *
 * <ul>
 * <li>{@code padre == null} y no transversal → sector (nivel 1).</li>
 * <li>{@code padre} es un sector → actividad principal (nivel 2).</li>
 * <li>{@code padre} es una actividad → etiqueta (nivel 3).</li>
 * <li>{@code padre == null} y transversal → grupo que vale para cualquier
 * sector (modalidad de trabajo); sus hijas son etiquetas transversales.</li>
 * </ul>
 *
 * Una sola tabla y no una por nivel porque {@code empresa.sector_id} apunta
 * aquí y la afinidad puntúa cuando un alumno marca un sector como interés
 * propio: separar los niveles rompería las dos cosas.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Etiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    /** De quién cuelga; null en las raíces (sectores y grupos transversales). */
    @ManyToOne
    private Etiqueta padre;

    /** Solo tiene sentido en una raíz: marca la que no es un sector. */
    @Column(nullable = false)
    private boolean transversal = false;

    public Etiqueta(String nombre) {
        this.nombre = nombre;
    }

    public Etiqueta(String nombre, Etiqueta padre, boolean transversal) {
        this.nombre = nombre;
        this.padre = padre;
        this.transversal = transversal;
    }

    /** 0 sector o grupo transversal, 1 actividad, 2 etiqueta. */
    public int nivel() {
        return padre == null ? 0 : padre.nivel() + 1;
    }
}
