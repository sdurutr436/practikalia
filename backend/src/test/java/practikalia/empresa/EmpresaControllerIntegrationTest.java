package practikalia.empresa;

import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmpresaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private EtiquetaRepository etiquetaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmpresaRepository empresaRepository;

    private Etiqueta sector;
    private Usuario profesor;

    @BeforeEach
    void setUp() {
        sector = etiquetaRepository.save(new Etiqueta("Tecnología"));
        profesor = usuarioRepository.save(new Usuario("prof@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.PROFESOR));
        usuarioRepository.save(new Usuario("alumno@iesejemplo.es", passwordEncoder.encode("Password123!"), Rol.ALUMNO));
    }

    private CrearEmpresaRequest requestValido(boolean publicada) {
        return new CrearEmpresaRequest("Acme", "desc", "dir", sector.getId(), List.of(), "obs", "Ana", "600", "ana@acme.com", publicada);
    }

    @Test
    void alumnoSoloVeEmpresasPublicadasConDtoPublico() throws Exception {
        Empresa publicada = new Empresa("Pública", "d", "dir", sector, "obs", "c", "t", "e", profesor);
        publicada.setPublicada(true);
        empresaRepository.save(publicada);
        empresaRepository.save(new Empresa("Privada", "d", "dir", sector, "obs", "c", "t", "e", profesor));

        mockMvc.perform(get("/api/empresas")
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Pública"))
                .andExpect(jsonPath("$[0].observaciones").doesNotExist());
    }

    @Test
    void profesorVeTodasLasEmpresasConDtoCompleto() throws Exception {
        empresaRepository.save(new Empresa("Privada", "d", "dir", sector, "obs", "c", "t", "e", profesor));

        mockMvc.perform(get("/api/empresas")
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].observaciones").value("obs"));
    }

    @Test
    void alumnoConsultandoEmpresaNoPublicadaDevuelve404() throws Exception {
        Empresa privada = empresaRepository.save(new Empresa("Privada", "d", "dir", sector, "obs", "c", "t", "e", profesor));

        mockMvc.perform(get("/api/empresas/" + privada.getId())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("EMPRESA_NO_ENCONTRADA"));
    }

    @Test
    void profesorCreaEmpresaYNacePrivada() throws Exception {
        mockMvc.perform(post("/api/empresas")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido(true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicada").value(false))
                .andExpect(jsonPath("$.creadaPorCorreo").value("prof@iesejemplo.es"));
    }

    @Test
    void alumnoNoPuedeCrearEmpresaDevuelve403() throws Exception {
        mockMvc.perform(post("/api/empresas")
                        .with(csrf())
                        .with(user("alumno@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_ALUMNO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido(false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void crearConSectorInexistenteDevuelve400() throws Exception {
        CrearEmpresaRequest request = new CrearEmpresaRequest("Acme", "desc", "dir", 999L, List.of(), null, null, null, null, false);

        mockMvc.perform(post("/api/empresas")
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ETIQUETA_NO_ENCONTRADA"));
    }

    @Test
    void profesorPublicaUnaEmpresaConPut() throws Exception {
        Empresa empresa = empresaRepository.save(new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor));

        mockMvc.perform(put("/api/empresas/" + empresa.getId())
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestValido(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicada").value(true));
    }

    @Test
    void profesorSubeImagenJpegValida() throws Exception {
        Empresa empresa = empresaRepository.save(new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor));
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02, 0x03};
        MockMultipartFile fichero = new MockMultipartFile("fichero", "foto.jpg", "image/jpeg", jpeg);

        mockMvc.perform(multipart("/api/empresas/" + empresa.getId() + "/imagen")
                        .file(fichero)
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagen").value(startsWith("/uploads/empresas/")));
    }

    @Test
    void subirFicheroDeMasDe5MbDevuelve400() throws Exception {
        Empresa empresa = empresaRepository.save(new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor));
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile fichero = new MockMultipartFile("fichero", "foto.jpg", "image/jpeg", grande);

        mockMvc.perform(multipart("/api/empresas/" + empresa.getId() + "/imagen")
                        .file(fichero)
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("IMAGEN_INVALIDA"));
    }

    @Test
    void subirExeRenombradoAJpgDevuelve400() throws Exception {
        Empresa empresa = empresaRepository.save(new Empresa("Acme", "d", "dir", sector, "obs", "c", "t", "e", profesor));
        byte[] exe = {0x4D, 0x5A, 0x00, 0x00};
        MockMultipartFile fichero = new MockMultipartFile("fichero", "foto.jpg", "image/jpeg", exe);

        mockMvc.perform(multipart("/api/empresas/" + empresa.getId() + "/imagen")
                        .file(fichero)
                        .with(csrf())
                        .with(user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("IMAGEN_INVALIDA"));
    }
}
