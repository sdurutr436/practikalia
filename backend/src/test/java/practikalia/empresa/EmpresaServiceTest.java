package practikalia.empresa;

import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmpresaServiceTest {

    private EmpresaRepository empresaRepository;
    private EtiquetaRepository etiquetaRepository;
    private UsuarioRepository usuarioRepository;
    private EmpresaService empresaService;

    private final Etiqueta sector = new Etiqueta("Tecnología");
    private final Usuario profesor = new Usuario("prof@iesejemplo.es", "hash", Rol.PROFESOR);

    @BeforeEach
    void setUp() {
        empresaRepository = mock(EmpresaRepository.class);
        etiquetaRepository = mock(EtiquetaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        empresaService = new EmpresaService(
                empresaRepository, etiquetaRepository, usuarioRepository, mock(ImagenEmpresaService.class));
    }

    private CrearEmpresaRequest request(Long sectorId, List<Long> etiquetaIds, boolean publicada) {
        return new CrearEmpresaRequest("Acme", "desc", "dir", sectorId, etiquetaIds, "obs", "Ana", "600", "ana@acme.com", publicada);
    }

    @Test
    void crearConSectorInexistenteLanzaExcepcion() {
        when(etiquetaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> empresaService.crear(request(99L, List.of(), false), "prof@iesejemplo.es"))
                .isInstanceOf(EmpresaException.class)
                .hasFieldOrPropertyWithValue("codigo", "ETIQUETA_NO_ENCONTRADA");
    }

    @Test
    void crearConEtiquetaInexistenteLanzaExcepcion() {
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(etiquetaRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> empresaService.crear(request(1L, List.of(2L), false), "prof@iesejemplo.es"))
                .isInstanceOf(EmpresaException.class)
                .hasFieldOrPropertyWithValue("codigo", "ETIQUETA_NO_ENCONTRADA");
    }

    @Test
    void crearIgnoraPublicadaDelRequestYNacePrivada() {
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(usuarioRepository.findByCorreo("prof@iesejemplo.es")).thenReturn(Optional.of(profesor));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));

        EmpresaProfesorDto response = empresaService.crear(request(1L, List.of(), true), "prof@iesejemplo.es");

        assertThat(response.publicada()).isFalse();
        assertThat(response.creadaPorCorreo()).isEqualTo("prof@iesejemplo.es");
    }

    @Test
    void actualizarPublicaLaEmpresaSegunElRequest() {
        Empresa empresa = new Empresa("Acme", "d", "dir", sector, null, null, null, null, profesor);
        when(empresaRepository.findById(5L)).thenReturn(Optional.of(empresa));
        when(etiquetaRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));

        EmpresaProfesorDto response = empresaService.actualizar(5L, request(1L, List.of(), true));

        assertThat(response.publicada()).isTrue();
        assertThat(response.descripcion()).isEqualTo("desc");
    }

    @Test
    void obtenerParaAlumnoConEmpresaNoPublicadaLanzaNoEncontrada() {
        Empresa empresa = new Empresa("Acme", null, null, sector, null, null, null, null, profesor);
        when(empresaRepository.findById(7L)).thenReturn(Optional.of(empresa));

        assertThatThrownBy(() -> empresaService.obtenerParaAlumno(7L))
                .isInstanceOf(EmpresaException.class)
                .hasFieldOrPropertyWithValue("codigo", "EMPRESA_NO_ENCONTRADA");
    }

    @Test
    void listarParaAlumnoSoloDevuelveEmpresasPublicadas() {
        Empresa publicada = new Empresa("Acme", null, null, sector, "obs", null, null, null, profesor);
        publicada.setPublicada(true);
        when(empresaRepository.findByPublicadaTrue()).thenReturn(List.of(publicada));

        List<EmpresaAlumnoDto> resultado = empresaService.listarParaAlumno();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("Acme");
    }
}
