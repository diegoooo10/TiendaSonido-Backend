package tienda_sonido.usuario_service.mapper;

import org.springframework.stereotype.Component;

import tienda_sonido.usuario_service.dto.UsuarioRequest;
import tienda_sonido.usuario_service.dto.UsuarioResponse;
import tienda_sonido.usuario_service.dto.UsuarioUpdateRequest;
import tienda_sonido.usuario_service.model.Usuario;

@Component 
public class UsuarioMapper {

    public Usuario toEntity(UsuarioRequest request) {
        Usuario usuarioNuevo = new Usuario();

        usuarioNuevo.setNombre(request.getNombre());
        usuarioNuevo.setApellido(request.getApellido());
        usuarioNuevo.setCorreo(request.getCorreo());
        usuarioNuevo.setDireccion(request.getDireccion());
        usuarioNuevo.setFechaNacimiento(request.getFechaNacimiento());
        usuarioNuevo.setRun(request.getRun());
        
        return usuarioNuevo;
    }

    public UsuarioResponse toResponse (Usuario usuario) {
        return new UsuarioResponse
            (usuario.getId(),
            usuario.getRol(),
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getCorreo(),
            usuario.getRun(),
            usuario.getFechaNacimiento(),
            usuario.getDireccion());
    }

    public void actualizarUsuario(UsuarioUpdateRequest request, Usuario usuario) {
        usuario.setRun(request.getRun());
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setCorreo(request.getCorreo());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setRol(request.getRol());
        usuario.setDireccion(request.getDireccion());
    }
    
}
