package practikalia.afinidad;

import practikalia.empresa.Empresa;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaDto;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class AfinidadServiceTest {

    private final AfinidadService afinidadService = new AfinidadService();

    private final Etiqueta java = etiquetaConId(1L, "Java");
    private final Etiqueta redes = etiquetaConId(2L, "Redes");
    private final Etiqueta diseno = etiquetaConId(3L, "Diseño");
    private final Etiqueta tecnologia = etiquetaConId(4L, "Tecnología");

    private static Etiqueta etiquetaConId(Long id, String nombre) {
        Etiqueta etiqueta = new Etiqueta(nombre);
        etiqueta.setId(id);
        return etiqueta;
    }

    private static Usuario alumnoCon(Etiqueta... etiquetas) {
        Usuario alumno = new Usuario("alumno@iesejemplo.es", "hash", Rol.ALUMNO);
        alumno.setEtiquetas(List.of(etiquetas));
        return alumno;
    }

    private static Empresa empresaCon(Etiqueta sector, Etiqueta... etiquetas) {
        Empresa empresa = new Empresa("Acme", null, null, sector, null, null, null, null, null);
        empresa.setEtiquetas(List.of(etiquetas));
        return empresa;
    }

    @Test
    void solapamientoParcialSobreElMinimoDeEtiquetas() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(java, diseno), empresaCon(tecnologia, java, redes));

        assertThat(resultado.score()).isEqualTo(0.5); // 1 coincidencia / min(2, 2)
        assertThat(resultado.etiquetasCoincidentes()).extracting(EtiquetaDto::nombre).containsExactly("Java");
        assertThat(resultado.sectorCoincide()).isFalse();
    }

    @Test
    void solapamientoNoCastigaALaEmpresaConMenosEtiquetasQueElAlumno() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(java, redes, diseno), empresaCon(tecnologia, java, redes));

        assertThat(resultado.score()).isEqualTo(1.0); // 2 coincidencias / min(3, 2)
    }

    @Test
    void empresaSinEtiquetasPuntuaCeroSinError() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(java), empresaCon(tecnologia));

        assertThat(resultado.score()).isEqualTo(0.0);
        assertThat(resultado.etiquetasCoincidentes()).isEmpty();
        assertThat(resultado.sectorCoincide()).isFalse();
    }

    @Test
    void bonusDeSectorSeSumaAlSolapamiento() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(java, tecnologia), empresaCon(tecnologia, java));

        assertThat(resultado.score()).isEqualTo(1.0 + AfinidadService.BONUS_SECTOR); // min(2, 1) = 1
        assertThat(resultado.sectorCoincide()).isTrue();
    }

    @Test
    void empresaSinEtiquetasConservaElBonusDeSector() {
        AfinidadEmpresaDto resultado = afinidadService.calcularScore(
                alumnoCon(tecnologia), empresaCon(tecnologia));

        assertThat(resultado.score()).isEqualTo(AfinidadService.BONUS_SECTOR);
        assertThat(resultado.sectorCoincide()).isTrue();
    }
}
