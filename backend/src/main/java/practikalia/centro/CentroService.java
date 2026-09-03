package practikalia.centro;

import practikalia.common.ImagenSubidaService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Configuración de la instancia: fila única, sin alta ni baja. `obtener()` es
 * público (lo consume el acceso, antes de haber sesión); el resto es de admin,
 * comprobado en {@code SecurityConfig}.
 */
@Service
public class CentroService {

    private final CentroRepository centroRepository;
    private final ImagenSubidaService imagenSubidaService;

    public CentroService(CentroRepository centroRepository, ImagenSubidaService imagenSubidaService) {
        this.centroRepository = centroRepository;
        this.imagenSubidaService = imagenSubidaService;
    }

    @Transactional(readOnly = true)
    public CentroDto obtener() {
        return CentroDto.de(buscar());
    }

    @Transactional
    public CentroDto actualizar(ActualizarCentroRequest request) {
        Centro centro = buscar();
        centro.setNombre(request.nombre().trim());
        centroRepository.save(centro);
        return CentroDto.de(centro);
    }

    @Transactional
    public CentroDto actualizarLogo(MultipartFile fichero) {
        Centro centro = buscar();
        centro.setLogo(imagenSubidaService.guardar(fichero, "centro"));
        centroRepository.save(centro);
        return CentroDto.de(centro);
    }

    /** La migración inserta la única fila (id 1); si no está, el despliegue está mal. */
    private Centro buscar() {
        return centroRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("La fila del centro no existe"));
    }
}
