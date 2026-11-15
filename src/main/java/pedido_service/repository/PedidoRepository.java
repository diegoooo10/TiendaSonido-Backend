package pedido_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pedido_service.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByNumeroPedido(String numeroPedido);

    boolean existsByNumeroPedido(String numeroPedido);

    Optional<Pedido> findByNumeroPedidoAndUsuarioCorreo(
            String numeroPedido,
            String usuarioCorreo
    );

    List<Pedido> findAllByUsuarioCorreoOrderByFechaCreacionDesc(
            String usuarioCorreo
    );
}
