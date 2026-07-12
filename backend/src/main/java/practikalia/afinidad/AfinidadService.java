package practikalia.afinidad;

import practikalia.empresa.Empresa;
import practikalia.empresa.EmpresaAlumnoDto;
import practikalia.etiqueta.Etiqueta;
import practikalia.etiqueta.EtiquetaDto;
import practikalia.usuario.Usuario;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class AfinidadService {

    // Constante provisional — la Fase 10 la sustituye por un peso real (w2·sector)
    static final double BONUS_SECTOR = 0.2;

    // Coeficiente de solapamiento |A∩B| / min(|A|,|B|), no Jaccard (c_fable.md, Fase 9)
    AfinidadEmpresaDto calcularScore(Usuario alumno, Empresa empresa) {
        Set<Long> etiquetasAlumno = alumno.getEtiquetas().stream().map(Etiqueta::getId).collect(Collectors.toSet());
        List<EtiquetaDto> coincidentes = empresa.getEtiquetas().stream()
                .filter(etiqueta -> etiquetasAlumno.contains(etiqueta.getId()))
                .map(EtiquetaDto::de)
                .toList();
        int minimo = Math.min(etiquetasAlumno.size(), empresa.getEtiquetas().size());
        double solapamiento = minimo == 0 ? 0.0 : (double) coincidentes.size() / minimo;
        boolean sectorCoincide = etiquetasAlumno.contains(empresa.getSector().getId());
        return new AfinidadEmpresaDto(EmpresaAlumnoDto.de(empresa),
                solapamiento + (sectorCoincide ? BONUS_SECTOR : 0.0), coincidentes, sectorCoincide);
    }
}
