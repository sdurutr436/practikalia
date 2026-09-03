package practikalia.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Sube una imagen validando su formato por la firma real de bytes (JPEG/PNG/
 * WebP), no por extensión ni Content-Type declarado; máximo 5 MB. La usan las
 * fotos de empresa y el logo del centro, cada una en su propio subdirectorio
 * de {@code uploads/}, así que vive en {@code common} y no en ninguna de las
 * dos features.
 */
@Service
public class ImagenSubidaService {

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] FIRMA_RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] FIRMA_WEBP = {'W', 'E', 'B', 'P'};

    private final Path raiz;

    ImagenSubidaService(@Value("${app.uploads-dir:uploads}") String directorioConfigurado) {
        this.raiz = Path.of(directorioConfigurado);
    }

    /**
     * @param subdirectorio p. ej. {@code "empresas"} o {@code "centro"}; separa los ficheros por feature.
     * @return la ruta relativa servida por nginx, p. ej. {@code /uploads/empresas/<uuid>.jpg}.
     */
    public String guardar(MultipartFile fichero, String subdirectorio) {
        if (fichero.isEmpty() || fichero.getSize() > TAMANO_MAXIMO) {
            throw ImagenException.invalida("La imagen supera el tamaño máximo permitido (5 MB)");
        }

        byte[] contenido = leer(fichero);
        String extension = detectarExtension(contenido);
        String nombre = UUID.randomUUID() + "." + extension;
        Path directorio = raiz.resolve(subdirectorio);
        escribir(directorio, nombre, contenido);
        return "/uploads/" + subdirectorio + "/" + nombre;
    }

    private byte[] leer(MultipartFile fichero) {
        try {
            return fichero.getBytes();
        } catch (IOException e) {
            throw ImagenException.invalida("No se pudo leer el fichero");
        }
    }

    private void escribir(Path directorio, String nombre, byte[] contenido) {
        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombre), contenido);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen en " + directorio, e);
        }
    }

    private String detectarExtension(byte[] contenido) {
        if (empiezaCon(contenido, FIRMA_JPEG)) {
            return "jpg";
        }
        if (empiezaCon(contenido, FIRMA_PNG)) {
            return "png";
        }
        if (esWebp(contenido)) {
            return "webp";
        }
        throw ImagenException.invalida("Formato de imagen no permitido (solo JPEG, PNG o WebP)");
    }

    private boolean esWebp(byte[] contenido) {
        return contenido.length >= 12 && empiezaCon(contenido, FIRMA_RIFF)
                && coincideEn(contenido, 8, FIRMA_WEBP);
    }

    private boolean empiezaCon(byte[] contenido, byte[] firma) {
        return coincideEn(contenido, 0, firma);
    }

    private boolean coincideEn(byte[] contenido, int offset, byte[] firma) {
        if (contenido.length < offset + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if (contenido[offset + i] != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
