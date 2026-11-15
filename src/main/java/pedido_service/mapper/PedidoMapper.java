package pedido_service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import pedido_service.dto.PedidoResponseDTO;
import pedido_service.dto.PedidoResumenDTO;
import pedido_service.dto.ProductoPedidoResponseDTO;
import pedido_service.model.DetallePedido;
import pedido_service.model.Pedido;

@Component
public class PedidoMapper {

    public PedidoResponseDTO toResponseDTO(Pedido pedido) {

        PedidoResponseDTO dto = new PedidoResponseDTO();

        dto.setId(pedido.getId());
        dto.setNumeroPedido(pedido.getNumeroPedido());
        dto.setUsuarioCorreo(pedido.getUsuarioCorreo());
        dto.setFechaCreacion(pedido.getFechaCreacion());
        dto.setTotal(pedido.getTotal());
        dto.setEstado(pedido.getEstado());

        List<ProductoPedidoResponseDTO> productos = pedido.getDetalles()
                .stream()
                .map(this::toProductoDTO)
                .toList();

        dto.setProductos(productos);

        return dto;
    }

    public PedidoResumenDTO toResumenDTO(Pedido pedido) {

        PedidoResumenDTO dto = new PedidoResumenDTO();

        dto.setId(pedido.getId());
        dto.setNumeroPedido(pedido.getNumeroPedido());
        dto.setFechaCreacion(pedido.getFechaCreacion());
        dto.setTotal(pedido.getTotal());
        dto.setEstado(pedido.getEstado());

        return dto;
    }

    private ProductoPedidoResponseDTO toProductoDTO(
            DetallePedido detalle) {

        ProductoPedidoResponseDTO dto = new ProductoPedidoResponseDTO();

        dto.setCodigoProducto(detalle.getCodigoProducto());
        dto.setNombreProducto(detalle.getNombreProducto());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setCantidad(detalle.getCantidad());
        dto.setSubtotal(detalle.getSubtotal());
        dto.setEstadoReserva(detalle.getEstadoReserva());

        return dto;
    }
}
