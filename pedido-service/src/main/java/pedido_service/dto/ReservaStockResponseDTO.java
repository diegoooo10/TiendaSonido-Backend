package pedido_service.dto;

import java.time.LocalDateTime;

import pedido_service.model.EstadoReserva;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReservaStockResponseDTO {

    private Long id;

    private String referencia;

    private String pedidoId;

    private String codigoProducto;

    private Integer cantidad;

    private EstadoReserva estado;

    private Integer stockDisponible;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}
