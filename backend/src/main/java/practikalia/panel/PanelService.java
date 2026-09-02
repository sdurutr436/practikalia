package practikalia.panel;

import practikalia.empresa.EmpresaRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PanelService {

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    public PanelService(EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Los cuatro contadores del centro, cada uno con su propio {@code count} en
     * base de datos: cuatro números no justifican materializar ni cachear nada.
     *
     * @return el resumen del centro entero, empresas sin publicar incluidas
     */
    @Transactional(readOnly = true)
    public ResumenCentroDto resumenCentro() {
        return new ResumenCentroDto(
                empresaRepository.countByPublicada(true),
                empresaRepository.countByPublicada(false),
                usuarioRepository.countByRolAndActivoTrue(Rol.ALUMNO),
                usuarioRepository.countActivosSinAsignacionAbierta(Rol.ALUMNO));
    }
}
