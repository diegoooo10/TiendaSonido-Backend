package pedido_service.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pedido_service.model.EstadoReserva;

@Getter
@Setter
@NoArgsConstructor
public class ProductoPedidoResponseDTO {

    private String codigoProducto;
    private String nombreProducto;
    private BigDecimal precioUnitario;
    private Integer cantidad;
    private BigDecimal subtotal;
    private EstadoReserva estadoReserva;
}
