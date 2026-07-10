package practikalia.interes;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.grado.Grado;
import practikalia.grado.GradoRepository;
import practikalia.usuario.ActualizarGradoRequest;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Flujo completo de Fase 6: marcar interés en un año, cambiar de año (Fase 5),
 * volver a marcar la misma empresa sin colisión, y desmarcar solo el año actual
 * dejando el anterior como histórico.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InteresRecurrenciaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario alumno;
    private Empresa empresa;
    private Grado grado;

    @BeforeEach
    void setUp() {
        Etiqueta sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        Usuario profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        grado = gradoRepository.save(new Grado("DAW"));
        alumno = new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO);
        alumno.setGrado(grado);
        alumno.setAnio(1);
        alumno = usuarioRepository.save(alumno);
        empresa = new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor);
        empresa.setPublicada(true);
        empresa = empresaRepository.save(empresa);
    }

    @Test
    void flujoCompletoDeInteresConCambioDeAnio() throws Exception {
        // Año 1: el alumno marca interés
        mockMvc.perform(put("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk());

        // Pasa a año 2 (Fase 5)
        mockMvc.perform(put("/api/usuarios/" + alumno.getId() + "/grado")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActualizarGradoRequest(grado.getId(), 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2));

        // Año 2: vuelve a marcar la misma empresa — registro nuevo, no colisiona
        mockMvc.perform(put("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk());

        // El profesor ve ambas marcas, cada una con su año
        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/interesados")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Desmarcar afecta solo al año actual (2), el año 1 queda como histórico
        mockMvc.perform(delete("/api/empresas/" + empresa.getId() + "/interes")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/empresas/" + empresa.getId() + "/interesados")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].anio").value(1));
    }
}
