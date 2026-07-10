package practikalia.empresa;

import practikalia.etiqueta.Etiqueta;
import practikalia.usuario.Usuario;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    private String imagen;

    private String direccion;

    @ManyToOne(optional = false)
    private Etiqueta sector;

    @ManyToMany
    @JoinTable(name = "empresa_etiqueta",
            joinColumns = @JoinColumn(name = "empresa_id"),
            inverseJoinColumns = @JoinColumn(name = "etiqueta_id"))
    private List<Etiqueta> etiquetas = new ArrayList<>();

    @Column(length = 2000)
    private String observaciones;

    private String contactoNombre;
    private String contactoTelefono;
    private String contactoEmail;

    @Column(nullable = false)
    private boolean publicada = false;

    @ManyToOne(optional = false)
    private Usuario creadaPor;

    @Column(nullable = false, updatable = false)
    private Instant fechaCreacion = Instant.now();

    public Empresa(String nombre, String descripcion, String direccion, Etiqueta sector, String observaciones,
            String contactoNombre, String contactoTelefono, String contactoEmail, Usuario creadaPor) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.direccion = direccion;
        this.sector = sector;
        this.observaciones = observaciones;
        this.contactoNombre = contactoNombre;
        this.contactoTelefono = contactoTelefono;
        this.contactoEmail = contactoEmail;
        this.creadaPor = creadaPor;
    }
}
