package practikalia.usuario;

import practikalia.grado.Grado;

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
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Column(nullable = false)
    private boolean esAdmin = false;

    @Column(nullable = false)
    private boolean debeCambiarContrasena = true;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private int intentosFallidos = 0;

    private Instant bloqueadoHasta;

    @ManyToOne
    private Grado grado;

    private Integer anio;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion = Instant.now();

    public Usuario(String correo, String contrasenaHash, Rol rol) {
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
    }
}
