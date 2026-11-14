package tienda_sonido.usuario_service.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import tienda_sonido.usuario_service.dto.LoginRequest;
import tienda_sonido.usuario_service.dto.LoginResponse;
import tienda_sonido.usuario_service.dto.UsuarioRequest;
import tienda_sonido.usuario_service.dto.UsuarioResponse;
import tienda_sonido.usuario_service.dto.UsuarioUpdateRequest;
import tienda_sonido.usuario_service.exception.CredencialesInvalidasException;
import tienda_sonido.usuario_service.exception.UsuarioDuplicadoException;
import tienda_sonido.usuario_service.exception.UsuarioNoEncontradoException;
import tienda_sonido.usuario_service.mapper.UsuarioMapper;
import tienda_sonido.usuario_service.model.Rol;
import tienda_sonido.usuario_service.model.Usuario;
import tienda_sonido.usuario_service.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

// ***** METODOS DE LOGIN ***** //

    public UsuarioResponse registrarUsuario(UsuarioRequest request) {
        Usuario nuevoUsuario = usuarioMapper.toEntity(request);

        if (usuarioRepository.existsByCorreo(nuevoUsuario.getCorreo())) {
            log.warn("Intento de utilizar un correo ya registrado");
            throw new UsuarioDuplicadoException("El correo ya está registrado");
        }

        if (usuarioRepository.existsByRun(nuevoUsuario.getRun())) {
            log.warn("Intento de utilizar un RUN ya registrado");
            throw new UsuarioDuplicadoException("El RUN ya está registrado");
        }

        nuevoUsuario.setRol(Rol.CLIENTE);
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        log.info("Usuario registrado con ID {}", usuarioGuardado.getId());
        UsuarioResponse usuarioRegistrado = usuarioMapper.toResponse(usuarioGuardado);

        return usuarioRegistrado;

    }

    public LoginResponse iniciarSesion(LoginRequest request) {
        Usuario usuarioLogin = usuarioRepository.findByCorreo(request.getCorreo()).orElseThrow(() -> new CredencialesInvalidasException("Credenciales incorrectas"));
        
        if (!passwordEncoder.matches(request.getPassword(), usuarioLogin.getPassword())) {
            log.warn("Intento de inicio de sesión con credenciales incorrectas");
            throw new CredencialesInvalidasException("Credenciales incorrectas");
        }

        log.info("Inicio de sesión correcto para usuario ID {}", usuarioLogin.getId());
        String token = jwtService.generarToken(usuarioLogin);

        return new LoginResponse(token);
    }
    
// ***** METODOS DE ADMIN ***** //

    public UsuarioResponse buscarUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));

        log.debug("Consulta de usuario ID {}", id);
        return usuarioMapper.toResponse(usuario);
    }

    public List<UsuarioResponse> listarUsuarios() {
        List<Usuario> listaUsuarios = usuarioRepository.findAll();

        List<UsuarioResponse> listaResponse = listaUsuarios.stream()
                                                .map(usuarioMapper::toResponse)
                                                .toList();

        log.info("Listado de usuarios consultado: {} registros", listaResponse.size());
        return listaResponse;
    }

    public UsuarioResponse actualizarUsuario(Long id, UsuarioUpdateRequest update) {
        Usuario usuarioActualizar = usuarioRepository.findById(id).orElseThrow(() -> new UsuarioNoEncontradoException("Usuario no encontrado"));

            if (!usuarioActualizar.getCorreo().equals(update.getCorreo()) && usuarioRepository.existsByCorreo(update.getCorreo())) {
                log.warn("Intento de utilizar un correo ya registrado");
            throw new UsuarioDuplicadoException("El correo ya está registrado");
            }

            if (!usuarioActualizar.getRun().equals(update.getRun()) && usuarioRepository.existsByRun(update.getRun())) {
                log.warn("Intento de utilizar un RUN ya registrado");
            throw new UsuarioDuplicadoException("El RUN ya está registrado");
            }
        
        usuarioMapper.actualizarUsuario(update, usuarioActualizar);

        Usuario usuarioActualizado = usuarioRepository.save(usuarioActualizar);

        log.info("Usuario actualizado ID {}", id);
        return usuarioMapper.toResponse(usuarioActualizado);
    }

    public void eliminarUsuario(Long id) {

        if (!usuarioRepository.existsById(id)) {
            throw new UsuarioNoEncontradoException("Usuario no encontrado");
        }

        usuarioRepository.deleteById(id);
        log.info("Usuario eliminado ID {}", id);
    }
}
