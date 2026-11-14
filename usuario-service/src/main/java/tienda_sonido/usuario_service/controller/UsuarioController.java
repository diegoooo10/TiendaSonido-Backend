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
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.CollectionModel;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Tag(name = "Usuarios", description = "Registro, autenticación y administración de usuarios")
@RestController
@RequestMapping ("/api/v1/tienda-sonido")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Registrar usuario")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "Usuario registrado"), @ApiResponse(responseCode = "400", description = "Datos inválidos"), @ApiResponse(responseCode = "409", description = "Correo o RUN duplicado")})
    @PostMapping("/registro")
    public ResponseEntity<EntityModel<UsuarioResponse>> registrarUsuario(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse usuarioRegistrado = usuarioService.registrarUsuario(request);
        
        return ResponseEntity.status(201).body(conEnlaces(usuarioRegistrado));
    }

    @Operation(summary = "Iniciar sesión y obtener JWT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Autenticación correcta"), @ApiResponse(responseCode = "401", description = "Credenciales inválidas")})
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = usuarioService.iniciarSesion(request);
        
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Buscar usuario por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuario encontrado"), @ApiResponse(responseCode = "404", description = "Usuario inexistente")})
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<EntityModel<UsuarioResponse>> buscarUsuarioPorId(@PathVariable Long id) {
        UsuarioResponse usuario = usuarioService.buscarUsuarioPorId(id);

        return ResponseEntity.ok(conEnlaces(usuario));
    }

    @Operation(summary = "Listar usuarios", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/usuarios")
    public ResponseEntity<CollectionModel<EntityModel<UsuarioResponse>>> listarUsuarios() {
        List<UsuarioResponse> usuarios = usuarioService.listarUsuarios();

        return ResponseEntity.ok(CollectionModel.of(usuarios.stream().map(this::conEnlaces).toList(),
                linkTo(methodOn(UsuarioController.class).listarUsuarios()).withSelfRel()));
    }

    @Operation(summary = "Actualizar usuario", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuario actualizado"), @ApiResponse(responseCode = "404", description = "Usuario inexistente"), @ApiResponse(responseCode = "409", description = "Correo o RUN duplicado")})
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<EntityModel<UsuarioResponse>> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest update) {
        UsuarioResponse usuarioActualizado = usuarioService.actualizarUsuario(id, update);

        return ResponseEntity.ok(conEnlaces(usuarioActualizado));
    }

    @Operation(summary = "Eliminar usuario", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({@ApiResponse(responseCode = "204", description = "Usuario eliminado"), @ApiResponse(responseCode = "404", description = "Usuario inexistente")})
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);

        return ResponseEntity.noContent().build();
    }
    private EntityModel<UsuarioResponse> conEnlaces(UsuarioResponse usuario) {
        return EntityModel.of(usuario,
                linkTo(methodOn(UsuarioController.class).buscarUsuarioPorId(usuario.getId())).withSelfRel(),
                linkTo(methodOn(UsuarioController.class).listarUsuarios()).withRel("usuarios"));
    }
}
