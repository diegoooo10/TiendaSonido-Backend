package tienda_sonido.usuario_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tienda_sonido.usuario_service.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRun(String run);
}
