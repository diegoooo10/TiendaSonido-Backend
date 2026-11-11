package producto_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Solicitud de reserva de stock para un pedido externo")
public class ReservaStockRequestDTO {

    @Schema(description = "Identificador del pedido en el microservicio Pedidos", example = "PED-2026-0001")
    @NotBlank(message = "El pedidoId es obligatorio")
    @Size(max = 100, message = "El pedidoId no puede superar los 100 caracteres")
    private String pedidoId;

    @Schema(description = "Codigo comercial del producto", example = "GA001")
    @NotBlank(message = "El codigo del producto es obligatorio")
    @Size(max = 20, message = "El codigo del producto no puede superar los 20 caracteres")
    private String codigoProducto;

    @Schema(description = "Unidades a reservar", example = "2")
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @Schema(description = "Clave idempotente de la operacion. No se pueden reutilizar referencias con datos distintos", example = "PED-2026-0001-GA001")
    @NotBlank(message = "La referencia es obligatoria")
    @Size(max = 100, message = "La referencia no puede superar los 100 caracteres")
    private String referencia;
}
