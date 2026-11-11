package producto_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Datos para actualizar el stock fisico de un producto")
public class ActualizarStockRequestDTO {

    @Schema(description = "Nuevo stock fisico. No puede ser menor al stock reservado", example = "20")
    @NotNull(message = "El stock fisico es obligatorio")
    @Min(value = 0, message = "El stock fisico no puede ser negativo")
    private Integer stockFisico;

    @Schema(description = "Umbral de stock critico. Si se omite se conserva el actual", example = "3")
    @Min(value = 0, message = "El stock critico no puede ser negativo")
    private Integer stockCritico;
}
