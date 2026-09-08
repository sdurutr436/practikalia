package practikalia.etiqueta;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtiquetaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EtiquetaRepository etiquetaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final RequestPostProcessor comoAdmin =
            user("admin@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"),
                    new SimpleGrantedAuthority("ADMIN"));
    private final RequestPostProcessor comoProfesor =
            user("profesor@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    private final RequestPostProcessor comoAlumno =
            user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"));

    @Test
    void profesorListaElCatalogoCompleto() throws Exception {
        etiquetaRepository.save(new Etiqueta("Java"));
        etiquetaRepository.save(new Etiqueta("Python"));

        mockMvc.perform(get("/api/etiquetas").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Java"))
                .andExpect(jsonPath("$[1].nombre").value("Python"));
    }

    @Test
    void alumnoAccedeAlCatalogo() throws Exception {
        etiquetaRepository.save(new Etiqueta("Java"));

        mockMvc.perform(get("/api/etiquetas").with(comoAlumno))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void sinAutenticarNoAccedeAlCatalogo() throws Exception {
        mockMvc.perform(get("/api/etiquetas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminVeElArbolAnidadoConLosTransversalesAlFinal() throws Exception {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Informática", null, false));
        Etiqueta actividad = etiquetaRepository.save(new Etiqueta("Desarrollo web", sector, false));
        etiquetaRepository.save(new Etiqueta("Java", actividad, false));
        Etiqueta grupo = etiquetaRepository.save(new Etiqueta("Modalidad de trabajo", null, true));
        etiquetaRepository.save(new Etiqueta("Teletrabajo", grupo, false));

        mockMvc.perform(get("/api/etiquetas/arbol").with(comoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Informática"))
                .andExpect(jsonPath("$[0].transversal").value(false))
                .andExpect(jsonPath("$[0].hijas[0].nombre").value("Desarrollo web"))
                .andExpect(jsonPath("$[0].hijas[0].hijas[0].nombre").value("Java"))
                .andExpect(jsonPath("$[1].nombre").value("Modalidad de trabajo"))
                .andExpect(jsonPath("$[1].transversal").value(true))
                .andExpect(jsonPath("$[1].hijas[0].nombre").value("Teletrabajo"));
    }

    @Test
    void profesorSinAdminNoVeElArbol() throws Exception {
        mockMvc.perform(get("/api/etiquetas/arbol").with(comoProfesor))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreaSectorActividadYEtiqueta() throws Exception {
        crear("{\"nombre\":\"Sanidad\"}");
        crear("{\"nombre\":\"Enfermería\",\"padreId\":" + idDe("Sanidad") + "}");
        crear("{\"nombre\":\"Geriatría\",\"padreId\":" + idDe("Enfermería") + "}");

        mockMvc.perform(get("/api/etiquetas/arbol").with(comoAdmin))
                .andExpect(jsonPath("$[0].nombre").value("Sanidad"))
                .andExpect(jsonPath("$[0].hijas[0].nombre").value("Enfermería"))
                .andExpect(jsonPath("$[0].hijas[0].hijas[0].nombre").value("Geriatría"));
    }

    @Test
    void elGrupoTransversalNaceMarcadoYSusHijasNo() throws Exception {
        crear("{\"nombre\":\"Modalidad de trabajo\",\"transversal\":true}");
        crear("{\"nombre\":\"Teletrabajo\",\"padreId\":" + idDe("Modalidad de trabajo")
                + ",\"transversal\":true}");

        mockMvc.perform(get("/api/etiquetas/arbol").with(comoAdmin))
                .andExpect(jsonPath("$[0].transversal").value(true))
                .andExpect(jsonPath("$[0].hijas[0].transversal").value(false));
    }

    @Test
    void noSeCuelgaUnCuartoNivel() throws Exception {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Sanidad", null, false));
        Etiqueta actividad = etiquetaRepository.save(new Etiqueta("Enfermería", sector, false));
        Etiqueta hoja = etiquetaRepository.save(new Etiqueta("Geriatría", actividad, false));

        mockMvc.perform(post("/api/etiquetas").with(csrf()).with(comoAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Residencias\",\"padreId\":" + hoja.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("NIVEL_MAXIMO"));
    }

    @Test
    void noSeRepiteElNombreAunqueCambieLaCaja() throws Exception {
        etiquetaRepository.save(new Etiqueta("Java"));

        mockMvc.perform(post("/api/etiquetas").with(csrf()).with(comoAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"java\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ETIQUETA_REPETIDA"));
    }

    @Test
    void adminRenombra() throws Exception {
        Etiqueta etiqueta = etiquetaRepository.save(new Etiqueta("Jaba"));

        mockMvc.perform(put("/api/etiquetas/" + etiqueta.getId()).with(csrf()).with(comoAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Java\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Java"));
    }

    @Test
    void adminBorraLoQueEstaLibre() throws Exception {
        Etiqueta etiqueta = etiquetaRepository.save(new Etiqueta("Cobol"));

        mockMvc.perform(delete("/api/etiquetas/" + etiqueta.getId()).with(csrf()).with(comoAdmin))
                .andExpect(status().isNoContent());

        assertTrue(etiquetaRepository.findById(etiqueta.getId()).isEmpty());
    }

    @Test
    void noBorraUnSectorConHijas() throws Exception {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Sanidad", null, false));
        etiquetaRepository.save(new Etiqueta("Enfermería", sector, false));

        mockMvc.perform(delete("/api/etiquetas/" + sector.getId()).with(csrf()).with(comoAdmin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ETIQUETA_CON_HIJAS"));
    }

    @Test
    void noBorraUnaEtiquetaQueTieneUnAlumno() throws Exception {
        Etiqueta etiqueta = etiquetaRepository.save(new Etiqueta("Java"));
        Usuario alumno = new Usuario("interesado@iesejemplo.es", "hash", Rol.ALUMNO);
        alumno.setEtiquetas(List.of(etiqueta));
        usuarioRepository.save(alumno);

        mockMvc.perform(delete("/api/etiquetas/" + etiqueta.getId()).with(csrf()).with(comoAdmin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ETIQUETA_EN_USO"));
    }

    @Test
    void profesorSinAdminNoMantieneElCatalogo() throws Exception {
        mockMvc.perform(post("/api/etiquetas").with(csrf()).with(comoProfesor)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Java\"}"))
                .andExpect(status().isForbidden());
    }

    private void crear(String cuerpo) throws Exception {
        mockMvc.perform(post("/api/etiquetas").with(csrf()).with(comoAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo))
                .andExpect(status().isCreated());
    }

    private Long idDe(String nombre) {
        return etiquetaRepository.findByNombre(nombre).orElseThrow().getId();
    }
}
