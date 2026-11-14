package tienda_sonido.usuario_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import tienda_sonido.usuario_service.dto.*;
import tienda_sonido.usuario_service.exception.*;
import tienda_sonido.usuario_service.mapper.UsuarioMapper;
import tienda_sonido.usuario_service.model.*;
import tienda_sonido.usuario_service.repository.UsuarioRepository;

class UsuarioServiceTest {
    @Mock UsuarioRepository repository;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    UsuarioService service;
    UsuarioMapper mapper = new UsuarioMapper();

    @BeforeEach
    void preparar() {
        MockitoAnnotations.openMocks(this);
        service = new UsuarioService(repository, mapper, encoder, jwt);
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(1L); u.setCorreo("test@example.com"); u.setRun("12345678-9");
        u.setPassword("hash"); u.setRol(Rol.CLIENTE); u.setNombre("Ana");
        u.setApellido("Pérez"); u.setFechaNacimiento(LocalDate.of(2000, 1, 1));
        u.setDireccion("Calle 1"); return u;
    }

    private UsuarioRequest registro() {
        Usuario u = usuario();
        return new UsuarioRequest("clave", u.getNombre(), u.getApellido(), u.getCorreo(),
                u.getRun(), u.getFechaNacimiento(), u.getDireccion());
    }

    private UsuarioUpdateRequest actualizacion() {
        Usuario u = usuario();
        return new UsuarioUpdateRequest(u.getRun(), "Ana María", u.getApellido(),
                u.getCorreo(), u.getFechaNacimiento(), Rol.ADMIN, u.getDireccion());
    }

    @Test void registrarUsuarioCorrectamente() {
        when(encoder.encode("clave")).thenReturn("hash");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0); u.setId(1L); return u;
        });
        UsuarioResponse result = service.registrarUsuario(registro());
        assertEquals(1L, result.getId());
        assertEquals(Rol.CLIENTE, result.getRol());
        verify(repository).save(argThat(u -> "hash".equals(u.getPassword())));
    }

    @Test void registroCorreoDuplicado() {
        when(repository.existsByCorreo(anyString())).thenReturn(true);
        assertThrows(UsuarioDuplicadoException.class, () -> service.registrarUsuario(registro()));
        verify(repository, never()).save(any());
    }

    @Test void registroRunDuplicado() {
        when(repository.existsByRun(anyString())).thenReturn(true);
        assertThrows(UsuarioDuplicadoException.class, () -> service.registrarUsuario(registro()));
    }

    @Test void loginCorrecto() {
        when(repository.findByCorreo("test@example.com")).thenReturn(Optional.of(usuario()));
        when(encoder.matches("clave", "hash")).thenReturn(true);
        when(jwt.generarToken(any(Usuario.class))).thenReturn("token-prueba");
        assertEquals("token-prueba", service.iniciarSesion(new LoginRequest("test@example.com", "clave")).getToken());
    }

    @Test void loginCorreoInexistente() {
        assertThrows(CredencialesInvalidasException.class,
                () -> service.iniciarSesion(new LoginRequest("nadie@example.com", "clave")));
    }

    @Test void loginPasswordIncorrecta() {
        when(repository.findByCorreo("test@example.com")).thenReturn(Optional.of(usuario()));
        assertThrows(CredencialesInvalidasException.class,
                () -> service.iniciarSesion(new LoginRequest("test@example.com", "incorrecta")));
    }

    @Test void buscarUsuarioExistente() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario()));
        assertEquals(1L, service.buscarUsuarioPorId(1L).getId());
    }

    @Test void buscarUsuarioInexistente() {
        assertThrows(UsuarioNoEncontradoException.class, () -> service.buscarUsuarioPorId(99L));
    }

    @Test void listarUsuarios() {
        when(repository.findAll()).thenReturn(List.of(usuario()));
        assertEquals(1, service.listarUsuarios().size());
    }

    @Test void actualizarUsuarioCorrectamente() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        UsuarioResponse response = service.actualizarUsuario(1L, actualizacion());
        assertEquals("Ana María", response.getNombre());
        assertEquals(Rol.ADMIN, response.getRol());
    }

    @Test void actualizarUsuarioInexistente() {
        assertThrows(UsuarioNoEncontradoException.class, () -> service.actualizarUsuario(99L, actualizacion()));
    }

    @Test void actualizarCorreoDuplicado() {
        UsuarioUpdateRequest update = actualizacion(); update.setCorreo("otro@example.com");
        when(repository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(repository.existsByCorreo("otro@example.com")).thenReturn(true);
        assertThrows(UsuarioDuplicadoException.class, () -> service.actualizarUsuario(1L, update));
    }

    @Test void actualizarRunDuplicado() {
        UsuarioUpdateRequest update = actualizacion(); update.setRun("99999999-9");
        when(repository.findById(1L)).thenReturn(Optional.of(usuario()));
        when(repository.existsByRun("99999999-9")).thenReturn(true);
        assertThrows(UsuarioDuplicadoException.class, () -> service.actualizarUsuario(1L, update));
    }

    @Test void eliminarUsuarioExistente() {
        when(repository.existsById(1L)).thenReturn(true);
        service.eliminarUsuario(1L);
        verify(repository).deleteById(1L);
    }

    @Test void eliminarUsuarioInexistente() {
        assertThrows(UsuarioNoEncontradoException.class, () -> service.eliminarUsuario(99L));
        verify(repository, never()).deleteById(anyLong());
    }
}
