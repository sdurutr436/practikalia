package practikalia.interes;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaException;
import practikalia.empresa.EmpresaRepository;
import practikalia.etiqueta.Etiqueta;
import practikalia.grado.Grado;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InteresServiceTest {

    private InteresRepository interesRepository;
    private UsuarioRepository usuarioRepository;
    private EmpresaRepository empresaRepository;
    private InteresService interesService;

    private final Grado grado = new Grado("DAW");
    private final Usuario alumno = usuarioConId(1L, "alumno@iesejemplo.es", Rol.ALUMNO);
    private final Usuario profesor = usuarioConId(2L, "prof@iesejemplo.es", Rol.PROFESOR);
    private final Empresa empresa = new Empresa("Acme", null, null, new Etiqueta("Tecnología"), null, null, null, null, profesor);

    {
        grado.setId(20L);
        empresa.setId(10L);
        empresa.setPublicada(true);
        alumno.setGrado(grado);
        alumno.setAnio(1);
    }

    @BeforeEach
    void setUp() {
        interesRepository = mock(InteresRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        empresaRepository = mock(EmpresaRepository.class);
        interesService = new InteresService(interesRepository, usuarioRepository, empresaRepository);
        when(usuarioRepository.findByCorreo("alumno@iesejemplo.es")).thenReturn(Optional.of(alumno));
        when(usuarioRepository.findByCorreo("prof@iesejemplo.es")).thenReturn(Optional.of(profesor));
        when(empresaRepository.findById(10L)).thenReturn(Optional.of(empresa));
    }

    private static Usuario usuarioConId(Long id, String correo, Rol rol) {
        Usuario usuario = new Usuario(correo, "hash", rol);
        usuario.setId(id);
        return usuario;
    }

    @Test
    void marcarInteresValidoGuardaSnapshotDeGradoYAnio() {
        interesService.marcar(10L, "alumno@iesejemplo.es");

        ArgumentCaptor<Interes> captor = ArgumentCaptor.forClass(Interes.class);
        verify(interesRepository).save(captor.capture());
        assertThat(captor.getValue().getGrado().getNombre()).isEqualTo("DAW");
        assertThat(captor.getValue().getAnio()).isEqualTo(1);
    }

    @Test
    void marcarSinGradoEnPerfilLanzaExcepcion() {
        alumno.setGrado(null);
        alumno.setAnio(null);

        assertThatThrownBy(() -> interesService.marcar(10L, "alumno@iesejemplo.es"))
                .isInstanceOf(InteresException.class)
                .hasFieldOrPropertyWithValue("codigo", "ALUMNO_SIN_GRADO");
    }

    @Test
    void marcarDosVecesMismoAnioNoFallaNiDuplica() {
        when(interesRepository.findByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(1L, 10L, 20L, 1))
                .thenReturn(Optional.of(new Interes(alumno, empresa, grado, 1)));

        assertThatCode(() -> interesService.marcar(10L, "alumno@iesejemplo.es")).doesNotThrowAnyException();
        verify(interesRepository, never()).save(any(Interes.class));
    }

    @Test
    void marcarEnEmpresaNoPublicadaLanzaExcepcion() {
        empresa.setPublicada(false);

        assertThatThrownBy(() -> interesService.marcar(10L, "alumno@iesejemplo.es"))
                .isInstanceOf(EmpresaException.class)
                .hasFieldOrPropertyWithValue("codigo", "EMPRESA_NO_ENCONTRADA");
    }

    @Test
    void cambiarDeAnioYVolverAMarcarCreaRegistroNuevo() {
        when(interesRepository.findByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(1L, 10L, 20L, 1))
                .thenReturn(Optional.of(new Interes(alumno, empresa, grado, 1)));
        alumno.setAnio(2);

        interesService.marcar(10L, "alumno@iesejemplo.es");

        ArgumentCaptor<Interes> captor = ArgumentCaptor.forClass(Interes.class);
        verify(interesRepository).save(captor.capture());
        assertThat(captor.getValue().getAnio()).isEqualTo(2);
    }

    @Test
    void desmarcarExistenteBorraLaMarca() {
        Interes interes = new Interes(alumno, empresa, grado, 1);
        when(interesRepository.findByAlumnoIdAndEmpresaIdAndGradoIdAndAnio(1L, 10L, 20L, 1))
                .thenReturn(Optional.of(interes));

        interesService.desmarcar(10L, "alumno@iesejemplo.es");

        verify(interesRepository).delete(interes);
    }

    @Test
    void desmarcarNoExistenteNoFalla() {
        assertThatCode(() -> interesService.desmarcar(10L, "alumno@iesejemplo.es")).doesNotThrowAnyException();
        verify(interesRepository, never()).delete(any(Interes.class));
    }

    @Test
    void profesorMarcandoLanzaAccesoDenegado() {
        assertThatThrownBy(() -> interesService.marcar(10L, "prof@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }

    @Test
    void profesorDesmarcandoLanzaAccesoDenegado() {
        assertThatThrownBy(() -> interesService.desmarcar(10L, "prof@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }

    @Test
    void listarPorAlumnoComoOtroAlumnoLanzaAccesoDenegado() {
        when(usuarioRepository.findByCorreo("otro@iesejemplo.es"))
                .thenReturn(Optional.of(usuarioConId(3L, "otro@iesejemplo.es", Rol.ALUMNO)));

        assertThatThrownBy(() -> interesService.listarPorAlumno(1L, false, "otro@iesejemplo.es"))
                .isInstanceOf(UsuarioException.class)
                .hasFieldOrPropertyWithValue("codigo", "ACCESO_DENEGADO");
    }
}
