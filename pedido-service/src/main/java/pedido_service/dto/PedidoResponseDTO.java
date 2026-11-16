package pedido_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pedido_service.model.EstadoPedido;

@Getter
@Setter
@NoArgsConstructor
public class PedidoResponseDTO {

    private Long id;
    private String numeroPedido;
    private String usuarioCorreo;
    private LocalDateTime fechaCreacion;
    private BigDecimal total;
    private EstadoPedido estado;
    private List<ProductoPedidoResponseDTO> productos;
}
