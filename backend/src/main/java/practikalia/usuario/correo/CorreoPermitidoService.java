package practikalia.usuario.correo;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de la whitelist desde la pantalla de configuración: la parte que se
 * añade a mano, correo a correo. `allowed.domains` sigue siendo configuración
 * de despliegue y no se toca aquí — {@link practikalia.usuario.UsuarioService}
 * acepta un correo si su dominio está permitido **o** si está en esta lista.
 */
@Service
public class CorreoPermitidoService {

    private final CorreoPermitidoRepository correoPermitidoRepository;

    public CorreoPermitidoService(CorreoPermitidoRepository correoPermitidoRepository) {
        this.correoPermitidoRepository = correoPermitidoRepository;
    }

    @Transactional(readOnly = true)
    public List<CorreoPermitidoDto> listar() {
        return correoPermitidoRepository.findAll(Sort.by("correo")).stream()
                .map(CorreoPermitidoDto::de)
                .toList();
    }

    /** El correo se normaliza a minúsculas: el login compara exacto, y una mayúscula suelta lo dejaría fuera. */
    @Transactional
    public CorreoPermitidoDto crear(CrearCorreoPermitidoRequest request) {
        String correo = request.correo().trim().toLowerCase();
        if (correoPermitidoRepository.existsByCorreo(correo)) {
            throw CorreoPermitidoException.yaExiste();
        }
        return CorreoPermitidoDto.de(correoPermitidoRepository.save(new CorreoPermitido(correo)));
    }

    /** No borra al usuario que ya se dio de alta con ese correo, pero le impide volver a entrar. */
    @Transactional
    public void borrar(Long id) {
        if (!correoPermitidoRepository.existsById(id)) {
            throw CorreoPermitidoException.noEncontrado();
        }
        correoPermitidoRepository.deleteById(id);
    }
}
