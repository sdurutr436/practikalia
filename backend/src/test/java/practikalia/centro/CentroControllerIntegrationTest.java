package practikalia.centro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CentroControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CentroRepository centroRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RequestPostProcessor comoAdmin = user("admin@iesejemplo.es")
            .authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"), new SimpleGrantedAuthority("ADMIN"));
    private final RequestPostProcessor comoProfesor =
            user("prof@iesejemplo.es").authorities(new SimpleGrantedAuthority("ROLE_PROFESOR"));

    @Test
    void seConsultaSinSesion() throws Exception {
        mockMvc.perform(get("/api/centro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Practikalia"))
                .andExpect(jsonPath("$.logo").doesNotExist());
    }

    @Test
    void unProfesorNoAdminNoPuedeRenombrarlo() throws Exception {
        mockMvc.perform(put("/api/centro")
                        .with(csrf())
                        .with(comoProfesor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActualizarCentroRequest("IES Otro"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void unAdminLoRenombra() throws Exception {
        mockMvc.perform(put("/api/centro")
                        .with(csrf())
                        .with(comoAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActualizarCentroRequest("IES Mi Dominio"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("IES Mi Dominio"));

        assertThat(centroRepository.findById(1L).orElseThrow().getNombre()).isEqualTo("IES Mi Dominio");
    }

    @Test
    void unAdminLeSubeElLogo() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};
        MockMultipartFile fichero = new MockMultipartFile("fichero", "logo.jpg", "image/jpeg", jpeg);

        mockMvc.perform(multipart("/api/centro/logo").file(fichero).with(csrf()).with(comoAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logo").value(startsWith("/uploads/centro/")));
    }

    @Test
    void unProfesorNoAdminNoPuedeSubirElLogo() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};
        MockMultipartFile fichero = new MockMultipartFile("fichero", "logo.jpg", "image/jpeg", jpeg);

        mockMvc.perform(multipart("/api/centro/logo").file(fichero).with(csrf()).with(comoProfesor))
                .andExpect(status().isForbidden());
    }
}
