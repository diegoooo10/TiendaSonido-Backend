package pedido_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReservaStockRequestDTO {

    @NotBlank(message = "El pedidoId es obligatorio")
    @Size(max = 100)
    private String pedidoId;

    @NotBlank(message = "El codigo del producto es obligatorio")
    @Size(max = 20)
    private String codigoProducto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @NotBlank(message = "La referencia es obligatoria")
    @Size(max = 100)
    private String referencia;
}
