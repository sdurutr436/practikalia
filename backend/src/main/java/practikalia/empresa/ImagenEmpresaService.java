package practikalia.empresa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
class ImagenEmpresaService {

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] FIRMA_RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] FIRMA_WEBP = {'W', 'E', 'B', 'P'};

    private final Path directorio;

    ImagenEmpresaService(@Value("${app.uploads-dir:uploads}") String directorioConfigurado) {
        this.directorio = Path.of(directorioConfigurado, "empresas");
    }

    String guardar(MultipartFile fichero) {
        if (fichero.isEmpty() || fichero.getSize() > TAMANO_MAXIMO) {
            throw EmpresaException.imagenInvalida("La imagen supera el tamaño máximo permitido (5 MB)");
        }

        byte[] contenido = leer(fichero);
        String extension = detectarExtension(contenido);
        String nombre = UUID.randomUUID() + "." + extension;
        escribir(nombre, contenido);
        return "/uploads/empresas/" + nombre;
    }

    private byte[] leer(MultipartFile fichero) {
        try {
            return fichero.getBytes();
        } catch (IOException e) {
            throw EmpresaException.imagenInvalida("No se pudo leer el fichero");
        }
    }

    private void escribir(String nombre, byte[] contenido) {
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
        throw EmpresaException.imagenInvalida("Formato de imagen no permitido (solo JPEG, PNG o WebP)");
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
