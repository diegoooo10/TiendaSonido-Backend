package producto_service.dto;

import java.time.LocalDateTime;

import org.springframework.hateoas.RepresentationModel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estado del inventario de un producto")
public class InventarioResponseDTO extends RepresentationModel<InventarioResponseDTO> {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "1")
    private Long productoId;

    @Schema(example = "GA001")
    private String codigoProducto;

    @Schema(example = "Guitarra Acustica Folk")
    private String nombreProducto;

    @Schema(description = "Unidades fisicas en bodega", example = "8")
    private Integer stockFisico;

    @Schema(description = "Unidades apartadas por reservas activas", example = "2")
    private Integer stockReservado;

    @Schema(description = "stockFisico - stockReservado", example = "6")
    private Integer stockDisponible;

    @Schema(example = "3")
    private Integer stockCritico;

    @Schema(description = "Indica si el stock disponible esta en el umbral critico o por debajo", example = "false")
    private Boolean bajoStockCritico;

    @Schema(example = "2026-11-10T15:30:00")
    private LocalDateTime fechaActualizacion;
}
