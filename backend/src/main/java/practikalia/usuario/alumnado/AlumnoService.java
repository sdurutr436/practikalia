package practikalia.usuario.alumnado;

import practikalia.asignacion.Asignacion;
import practikalia.asignacion.AsignacionRepository;
import practikalia.asignacion.Curso;
import practikalia.common.PaginaDto;
import practikalia.grado.Grado;
import practikalia.grado.GradoDto;
import practikalia.grado.GradoException;
import practikalia.grado.GradoRepository;
import practikalia.usuario.Rol;
import practikalia.usuario.Usuario;
import practikalia.usuario.UsuarioException;
import practikalia.usuario.UsuarioRepository;
import practikalia.usuario.UsuarioService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Listado, edición e importación masiva de alumnado. Vive en un subpaquete de
 * {@code usuario} y no en uno propio porque la entidad sigue siendo
 * {@code Usuario}: aquí solo está la vista "alumno" de ella.
 */
@Service
public class AlumnoService {

    /** Cabecera de la plantilla, y a la vez el orden en el que se leen las columnas. */
    static final List<String> COLUMNAS = List.of(
            "nombre", "apellido1", "apellido2", "dni", "correo", "grado", "anio");

    private final UsuarioRepository usuarioRepository;
    private final AsignacionRepository asignacionRepository;
    private final GradoRepository gradoRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    public AlumnoService(
            UsuarioRepository usuarioRepository,
            AsignacionRepository asignacionRepository,
            GradoRepository gradoRepository,
            UsuarioService usuarioService,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.asignacionRepository = asignacionRepository;
        this.gradoRepository = gradoRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    /** @param activo {@code null} para la pastilla "Todos". */
    @Transactional(readOnly = true)
    public PaginaDto<AlumnoDto> listar(Boolean activo, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano,
                Sort.by(Sort.Direction.ASC, "apellido1", "nombre", "correo"));
        Page<Usuario> alumnos = activo == null
                ? usuarioRepository.findByRol(Rol.ALUMNO, pageable)
                : usuarioRepository.findByRolAndActivo(Rol.ALUMNO, activo, pageable);

        Map<Long, Asignacion> abiertas = asignacionesAbiertas(alumnos.getContent());
        return PaginaDto.de(alumnos, alumno -> de(alumno, abiertas.get(alumno.getId())));
    }

    /**
     * El alumnado del curso académico en marcha, que es lo que pinta la pantalla
     * de asignaciones: se asume que las prácticas se hacen en el curso de
     * matrícula, y ese curso no se repite. A diferencia de {@link #listar} aquí
     * no se filtra por {@code activo}: una cuenta sin confirmar sigue siendo un
     * alumno al que hay que buscarle empresa.
     *
     * @param anio     el curso que se quiere ver; {@code null} para el que está en marcha.
     * @param asignado {@code null} para la pastilla "Todas".
     */
    @Transactional(readOnly = true)
    public PaginaDto<AlumnoDto> listarDelCurso(
            Integer anio, Long gradoId, String texto, Boolean asignado, int pagina, int tamano) {
        int curso = anio == null ? Curso.actual() : anio;
        String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase() + "%";

        Page<Usuario> alumnos = usuarioRepository.buscarDelCurso(
                Rol.ALUMNO, curso, Curso.inicio(curso), Curso.inicio(curso + 1), gradoId, patron, asignado,
                PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.ASC, "apellido1", "nombre", "correo")));

        Map<Long, Asignacion> abiertas = asignacionesAbiertas(alumnos.getContent());
        return PaginaDto.de(alumnos, alumno -> de(alumno, abiertas.get(alumno.getId())));
    }

    /** Los cursos del selector. El actual entra siempre, aunque todavía no tenga a nadie. */
    @Transactional(readOnly = true)
    public CursosDto cursos() {
        int actual = Curso.actual();
        List<Integer> cursos = new ArrayList<>(usuarioRepository.cursosConAlumnado(Rol.ALUMNO));
        if (!cursos.contains(actual)) {
            cursos.add(actual);
            cursos.sort(Comparator.reverseOrder());
        }
        return new CursosDto(actual, cursos);
    }

    /**
     * Alta a mano desde el modal del listado. Nace confirmada, a diferencia de
     * las importadas: aquí los datos los acaba de teclear una persona en este
     * mismo formulario, así que no hay nada que revisar después.
     */
    @Transactional
    public AlumnoDto crear(FichaAlumnoRequest request) {
        String correo = request.correo().trim().toLowerCase();
        String dni = request.dni().trim().toUpperCase();

        if (!UsuarioService.dniValido(dni)) {
            throw UsuarioException.dniInvalido();
        }
        if (!usuarioService.dominioPermitido(correo)) {
            throw UsuarioException.correoDominioNoPermitido();
        }
        if (usuarioRepository.findByCorreo(correo).isPresent()) {
            throw UsuarioException.correoYaExiste();
        }
        if (usuarioRepository.existsByDni(dni)) {
            throw UsuarioException.dniYaRegistrado();
        }

        Usuario alumno = new Usuario(correo, passwordEncoder.encode(UsuarioService.contrasenaInicial(dni)), Rol.ALUMNO);
        aplicar(alumno, request, dni, correo);
        usuarioRepository.save(alumno);
        usuarioService.permitirCorreo(correo);

        return de(alumno, null);
    }

    @Transactional
    public AlumnoDto editar(Long id, FichaAlumnoRequest request) {
        Usuario alumno = buscarAlumno(id);
        String correo = request.correo().trim().toLowerCase();
        String dni = request.dni().trim().toUpperCase();

        if (!UsuarioService.dniValido(dni)) {
            throw UsuarioException.dniInvalido();
        }
        usuarioRepository.findByCorreo(correo).ifPresent(otro -> {
            if (!otro.getId().equals(id)) {
                throw UsuarioException.correoYaExiste();
            }
        });

        aplicar(alumno, request, dni, correo);
        usuarioRepository.save(alumno);

        return de(alumno, asignacionesAbiertas(List.of(alumno)).get(alumno.getId()));
    }

    private void aplicar(Usuario alumno, FichaAlumnoRequest request, String dni, String correo) {
        alumno.setNombre(request.nombre().trim());
        alumno.setApellido1(request.apellido1().trim());
        alumno.setApellido2(vacioANulo(request.apellido2()));
        alumno.setDni(dni);
        alumno.setCorreo(correo);
        alumno.setGrado(request.gradoId() == null
                ? null
                : gradoRepository.findById(request.gradoId()).orElseThrow(GradoException::noEncontrado));
        alumno.setAnio(request.anio());
    }

    /** La plantilla es solo la cabecera: con filas de ejemplo se acaban importando alumnos inventados. */
    public String plantillaCsv() {
        return String.join(",", COLUMNAS) + "\n";
    }

    /**
     * Alta masiva desde el CSV de la plantilla. Todo o nada: si una fila falla no
     * se crea ninguna. Los alumnos quedan {@code activo = false} para que el
     * centro revise los datos antes de confirmarlos uno a uno.
     */
    @Transactional
    public ImportacionDto importar(MultipartFile fichero) {
        List<String[]> filas = leer(fichero);
        List<Usuario> nuevos = new ArrayList<>();
        Set<String> correosDelFichero = new HashSet<>();
        Set<String> dnisDelFichero = new HashSet<>();

        for (int i = 0; i < filas.size(); i++) {
            int linea = i + 2; // +1 por la cabecera, +1 porque las líneas se cuentan desde 1
            nuevos.add(aUsuario(filas.get(i), linea, correosDelFichero, dnisDelFichero));
        }

        usuarioRepository.saveAll(nuevos);
        // Sin esto las cuentas se crean pero `login` las rechaza para siempre:
        // exige que el correo esté permitido, y el dominio solo abre la puerta.
        nuevos.forEach(alumno -> usuarioService.permitirCorreo(alumno.getCorreo()));
        return new ImportacionDto(nuevos.size());
    }

    private Usuario aUsuario(String[] campos, int linea, Set<String> correos, Set<String> dnis) {
        Map<String, String> fila = new HashMap<>();
        for (int i = 0; i < COLUMNAS.size(); i++) {
            fila.put(COLUMNAS.get(i), i < campos.length ? campos[i].trim() : "");
        }

        String dni = fila.get("dni").toUpperCase();
        String correo = fila.get("correo").toLowerCase();

        exigir(!fila.get("nombre").isBlank(), linea, "falta el nombre");
        exigir(!fila.get("apellido1").isBlank(), linea, "falta el primer apellido");
        exigir(UsuarioService.dniValido(dni), linea, "el DNI '" + fila.get("dni") + "' no es válido");
        exigir(correo.contains("@"), linea, "el correo '" + fila.get("correo") + "' no es válido");
        exigir(usuarioService.dominioPermitido(correo), linea,
                "el dominio de '" + correo + "' no está permitido en este centro");
        exigir(correos.add(correo), linea, "el correo '" + correo + "' está repetido en el fichero");
        exigir(dnis.add(dni), linea, "el DNI '" + dni + "' está repetido en el fichero");
        exigir(usuarioRepository.findByCorreo(correo).isEmpty(), linea, "ya existe una cuenta con el correo '" + correo + "'");
        exigir(!usuarioRepository.existsByDni(dni), linea, "ya existe una cuenta con el DNI '" + dni + "'");

        Grado grado = null;
        if (!fila.get("grado").isBlank()) {
            grado = gradoRepository.findByNombre(fila.get("grado"))
                    .orElseThrow(() -> AlumnoException.csvInvalido(
                            "Línea " + linea + ": la clase '" + fila.get("grado") + "' no existe"));
        }
        Integer anio = null;
        if (!fila.get("anio").isBlank()) {
            try {
                anio = Integer.valueOf(fila.get("anio"));
            } catch (NumberFormatException e) {
                throw AlumnoException.csvInvalido("Línea " + linea + ": el año '" + fila.get("anio") + "' no es un número");
            }
        }

        // La contraseña inicial es el DNI sin letra: el alumno la sabe sin que
        // nadie se la reparta, y el flag de cambio obligatorio sigue puesto.
        Usuario alumno = new Usuario(correo, passwordEncoder.encode(UsuarioService.contrasenaInicial(dni)), Rol.ALUMNO);
        alumno.setNombre(fila.get("nombre"));
        alumno.setApellido1(fila.get("apellido1"));
        alumno.setApellido2(vacioANulo(fila.get("apellido2")));
        alumno.setDni(dni);
        alumno.setGrado(grado);
        alumno.setAnio(anio);
        alumno.setActivo(false);
        return alumno;
    }

    /**
     * Lee el CSV a filas de campos. Acepta `;` además de `,` porque es lo que
     * pone Excel en español, y se come el BOM que escribe al guardar en UTF-8.
     *
     * ponytail: partido por separador a secas, sin comillas ni saltos de línea
     * dentro de un campo. Ninguna columna de la plantilla los admite; si algún
     * día entra una de texto libre, hay que pasar a un parser de verdad.
     */
    private List<String[]> leer(MultipartFile fichero) {
        List<String[]> filas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(fichero.getInputStream(), StandardCharsets.UTF_8))) {
            String cabecera = lector.readLine();
            if (cabecera == null) {
                throw AlumnoException.csvInvalido("El fichero está vacío");
            }
            cabecera = cabecera.replace("﻿", "").trim();
            char separador = cabecera.contains(";") ? ';' : ',';
            List<String> columnas = List.of(cabecera.toLowerCase().split(String.valueOf(separador), -1));
            if (!columnas.stream().map(String::trim).toList().equals(COLUMNAS)) {
                throw AlumnoException.csvInvalido(
                        "La cabecera debe ser exactamente: " + String.join(",", COLUMNAS));
            }

            String linea;
            while ((linea = lector.readLine()) != null) {
                if (!linea.isBlank()) {
                    filas.add(linea.split(String.valueOf(separador), -1));
                }
            }
        } catch (IOException e) {
            throw AlumnoException.csvInvalido("No se pudo leer el fichero");
        }
        if (filas.isEmpty()) {
            throw AlumnoException.csvInvalido("El fichero no tiene ninguna fila de datos");
        }
        return filas;
    }

    /** Una sola consulta para toda la página: si no, cada tarjeta haría la suya. */
    private Map<Long, Asignacion> asignacionesAbiertas(List<Usuario> alumnos) {
        List<Long> ids = alumnos.stream().map(Usuario::getId).toList();
        Map<Long, Asignacion> porAlumno = new HashMap<>();
        if (!ids.isEmpty()) {
            asignacionRepository.findByAlumnoIdInAndFechaFinIsNull(ids)
                    .forEach(asignacion -> porAlumno.put(asignacion.getAlumno().getId(), asignacion));
        }
        return porAlumno;
    }

    private Usuario buscarAlumno(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(UsuarioException::noEncontrado);
        if (usuario.getRol() != Rol.ALUMNO) {
            throw UsuarioException.noEncontrado();
        }
        return usuario;
    }

    private static void exigir(boolean condicion, int linea, String problema) {
        if (!condicion) {
            throw AlumnoException.csvInvalido("Línea " + linea + ": " + problema);
        }
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static AlumnoDto de(Usuario alumno, Asignacion abierta) {
        return new AlumnoDto(
                alumno.getId(),
                alumno.getNombre(),
                alumno.getApellido1(),
                alumno.getApellido2(),
                alumno.getDni(),
                alumno.getCorreo(),
                alumno.getGrado() == null ? null : GradoDto.de(alumno.getGrado()),
                alumno.getAnio(),
                alumno.isActivo(),
                abierta == null ? null : abierta.getEmpresa().getId(),
                abierta == null ? null : abierta.getEmpresa().getNombre(),
                abierta == null ? null : abierta.getTutorCentro().getId(),
                abierta == null ? null : nombreDe(abierta.getTutorCentro()));
    }

    /**
     * Nombre y apellidos de un tutor. Con el correo de respaldo: las cuentas de
     * profesor dadas de alta desde {@code POST /api/usuarios} nacen sin nombre.
     */
    private static String nombreDe(Usuario usuario) {
        String nombre = Stream.of(usuario.getNombre(), usuario.getApellido1(), usuario.getApellido2())
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
        return nombre.isBlank() ? usuario.getCorreo() : nombre;
    }
}
