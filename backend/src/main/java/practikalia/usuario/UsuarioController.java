package practikalia.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<CrearUsuarioResponse> crear(Authentication authentication, @Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioDto creador = usuarioService.buscarPorCorreo(authentication.getName());
        CrearUsuarioResponse response = usuarioService.crearUsuario(request, creador);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
