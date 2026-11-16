package pedido_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pedido_service.model.EstadoPedido;

@Getter
@Setter
@NoArgsConstructor
public class PedidoResumenDTO {

    private Long id;

    private String numeroPedido;

    private LocalDateTime fechaCreacion;

    private BigDecimal total;

    private EstadoPedido estado;
}
