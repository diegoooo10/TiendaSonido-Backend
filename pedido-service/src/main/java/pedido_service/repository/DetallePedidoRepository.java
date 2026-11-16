package pedido_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pedido_service.model.DetallePedido;

public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, Long> {

    List<DetallePedido> findByPedidoId(Long pedidoId);
}
