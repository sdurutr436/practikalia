package practikalia.panel;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PanelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;

    private final RequestPostProcessor comoProfesor =
            user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));
    private final RequestPostProcessor comoAlumno =
            user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"));

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        Usuario profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", "hash", Rol.PROFESOR));
        usuarioRepository.save(new Usuario("alumno@iesejemplo.es", "hash", Rol.ALUMNO));
        empresaRepository.save(new Empresa("Borrador", null, null, sector, null, null, null, null, profesor));
    }

    @Test
    void elProfesorVeLosCuatroContadores() throws Exception {
        mockMvc.perform(get("/api/panel/resumen").with(comoProfesor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresasPublicadas").value(0))
                .andExpect(jsonPath("$.empresasSinPublicar").value(1))
                .andExpect(jsonPath("$.alumnadoActivo").value(1))
                .andExpect(jsonPath("$.alumnadoSinAsignar").value(1));
    }

    @Test
    void elAlumnoNoVeElResumenDelCentro() throws Exception {
        mockMvc.perform(get("/api/panel/resumen").with(comoAlumno))
                .andExpect(status().isForbidden());
    }
}
