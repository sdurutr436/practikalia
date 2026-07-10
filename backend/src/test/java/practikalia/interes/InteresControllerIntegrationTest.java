package practikalia.interes;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.grado.Grado;
import practikalia.grado.GradoRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InteresControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private InteresRepository interesRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario profesor;
    private Usuario alumno;
    private Empresa empresa;
    private Grado grado;

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        grado = gradoRepository.save(new Grado("DAW"));
        alumno = new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO);
        alumno.setGrado(grado);
        alumno.setAnio(1);
        alumno = usuarioRepository.save(alumno);
        usuarioRepository.save(new Usuario("otro@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
        empresa = new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor);
        empresa.setPublicada(true);
        empresa = empresaRepository.save(empresa);
    }

    @Test
    void alumnoMarcaInteres() throws Exception {
        mockMvc.perform(put("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk());

        assertThat(interesRepository.findByEmpresaId(empresa.getId())).hasSize(1);
    }

    @Test
    void profesorMarcandoInteresDevuelve403() throws Exception {
        mockMvc.perform(put("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void marcarRepetidoMismoAnioSigueDevolviendo200SinDuplicar() throws Exception {
        interesRepository.save(new Interes(alumno, empresa, grado, 1));

        mockMvc.perform(put("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk());

        assertThat(interesRepository.findByEmpresaId(empresa.getId())).hasSize(1);
    }

    @Test
    void marcarSinGradoEnPerfilDevuelve400() throws Exception {
        mockMvc.perform(put("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("otro@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ALUMNO_SIN_GRADO"));
    }

    @Test
    void marcarEnEmpresaNoPublicadaDevuelve404() throws Exception {
        empresa.setPublicada(false);
        empresaRepository.save(empresa);

        mockMvc.perform(put("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("EMPRESA_NO_ENCONTRADA"));
    }

    @Test
    void desmarcarMarcadoYSinMarcarDevuelven204() throws Exception {
        interesRepository.save(new Interes(alumno, empresa, grado, 1));

        mockMvc.perform(delete("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isNoContent());
        assertThat(interesRepository.findByEmpresaId(empresa.getId())).isEmpty();

        mockMvc.perform(delete("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void profesorDesmarcandoInteresDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void profesorListaInteresadosConGradoYAnio() throws Exception {
        interesRepository.save(new Interes(alumno, empresa, grado, 1));

        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/interesados")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alumnoCorreo").value("alumno@iesejemplo.es"))
                .andExpect(jsonPath("$[0].gradoNombre").value("DAW"))
                .andExpect(jsonPath("$[0].anio").value(1));
    }

    @Test
    void alumnoListandoInteresadosDevuelve403() throws Exception {
        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/interesados")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void alumnoConsultaSusPropiosIntereses() throws Exception {
        interesRepository.save(new Interes(alumno, empresa, grado, 1));

        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/intereses")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].empresaNombre").value("Acme"));
    }

    @Test
    void alumnoConsultandoInteresesDeOtroDevuelve403() throws Exception {
        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/intereses")
                        .with(user("otro@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void profesorConsultaInteresesDeCualquierAlumno() throws Exception {
        interesRepository.save(new Interes(alumno, empresa, grado, 1));

        mockMvc.perform(get("/api/alumnos/" + alumno.getId() + "/intereses")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
