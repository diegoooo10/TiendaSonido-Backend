package tienda_sonido.usuario_service.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tienda_sonido.usuario_service.dto.LoginRequest;
import tienda_sonido.usuario_service.dto.LoginResponse;
import tienda_sonido.usuario_service.dto.UsuarioRequest;
import tienda_sonido.usuario_service.dto.UsuarioResponse;
import tienda_sonido.usuario_service.dto.UsuarioUpdateRequest;
import tienda_sonido.usuario_service.service.UsuarioService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping ("/api/v1/tienda-sonido")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrarUsuario(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse usuarioRegistrado = usuarioService.registrarUsuario(request);
        
        return ResponseEntity.status(201).body(usuarioRegistrado);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = usuarioService.iniciarSesion(request);
        
        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> buscarUsuarioPorId(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.buscarUsuarioPorId(id);

        return ResponseEntity.ok(usuario);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        List<UsuarioResponse> usuarios = usuarioService.listarUsuarios();

        return ResponseEntity.ok(usuarios);
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest update) {
        UsuarioResponse usuarioActualizado = usuarioService.actualizarUsuario(id, update);

        return ResponseEntity.ok(usuarioActualizado);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);

        return ResponseEntity.noContent().build();
    }
}
