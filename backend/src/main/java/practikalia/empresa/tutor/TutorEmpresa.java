package practikalia.empresa.tutor;

import practikalia.empresa.Empresa;

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
 * Personal de la empresa que ejerce de tutor del alumnado en prácticas. No es
 * una cuenta de la aplicación: no entra, no tiene rol y no se autentica; es un
 * dato de la ficha de su empresa.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class TutorEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Empresa empresa;

    @Column(nullable = false)
    private String nombre;

    private String cargo;

    private String telefono;

    private String correo;

    public TutorEmpresa(Empresa empresa, String nombre) {
        this.empresa = empresa;
        this.nombre = nombre;
    }
}
