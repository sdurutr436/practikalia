package practikalia.empresa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImagenEmpresaServiceTest {

    @TempDir
    Path tempDir;

    private ImagenEmpresaService servicio() {
        return new ImagenEmpresaService(tempDir.toString());
    }

    @Test
    void guardaUnJpegValidoYDevuelveRutaConExtensionCorrecta() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};
        MockMultipartFile fichero = new MockMultipartFile("fichero", "foto.jpg", "image/jpeg", jpeg);

        String ruta = servicio().guardar(fichero);

        assertThat(ruta).startsWith("/uploads/empresas/").endsWith(".jpg");
    }

    @Test
    void guardaUnWebpValidoPorFirmaRiffWebp() {
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        MockMultipartFile fichero = new MockMultipartFile("fichero", "foto.webp", "image/webp", webp);

        String ruta = servicio().guardar(fichero);

        assertThat(ruta).endsWith(".webp");
    }

    @Test
    void rechazaFicheroQueSuperaElTamanoMaximo() {
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile fichero = new MockMultipartFile("fichero", "foto.jpg", "image/jpeg", grande);

        assertThatThrownBy(() -> servicio().guardar(fichero))
                .isInstanceOf(EmpresaException.class)
                .hasFieldOrPropertyWithValue("codigo", "IMAGEN_INVALIDA");
    }

    @Test
    void rechazaUnEjecutableDisfrazadoDeJpegPorContenidoReal() {
        byte[] exe = {0x4D, 0x5A, 0x00, 0x00};
        MockMultipartFile fichero = new MockMultipartFile("fichero", "malware.jpg", "image/jpeg", exe);

        assertThatThrownBy(() -> servicio().guardar(fichero))
                .isInstanceOf(EmpresaException.class)
                .hasFieldOrPropertyWithValue("codigo", "IMAGEN_INVALIDA");
    }

    @Test
    void rechazaSvgPorNoSerNingunFormatoPermitido() {
        byte[] svg = "<svg onload=alert(1)></svg>".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile fichero = new MockMultipartFile("fichero", "foto.svg", "image/svg+xml", svg);

        assertThatThrownBy(() -> servicio().guardar(fichero))
                .isInstanceOf(EmpresaException.class)
                .hasFieldOrPropertyWithValue("codigo", "IMAGEN_INVALIDA");
    }
}
