package producto_service.dto;

import java.time.LocalDateTime;

import producto_service.model.EstadoReserva;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estado de una reserva de stock")
public class ReservaStockResponseDTO {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "PED-2026-0001-GA001")
    private String referencia;

    @Schema(example = "PED-2026-0001")
    private String pedidoId;

    @Schema(example = "GA001")
    private String codigoProducto;

    @Schema(example = "2")
    private Integer cantidad;

    @Schema(example = "RESERVADA")
    private EstadoReserva estado;

    @Schema(description = "Stock disponible luego de aplicar la operacion", example = "6")
    private Integer stockDisponible;

    @Schema(example = "2026-11-10T15:30:00")
    private LocalDateTime fechaCreacion;

    @Schema(example = "2026-11-10T15:30:00")
    private LocalDateTime fechaActualizacion;
}
