package practikalia.centro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuración de la instancia: fila única (id 1, sin generador — nadie
 * inserta ni borra centros) con el nombre y el logo que se ven en el acceso,
 * el marco y el título de la pestaña.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Centro {

    @Id
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String logo;
}
