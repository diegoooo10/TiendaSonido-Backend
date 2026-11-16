package pedido_service.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CrearPedidoRequestDTO {

    @NotEmpty(message = "El pedido debe contener al menos un producto")
    @Valid
    private List<ProductoPedidoRequestDTO> productos;
}
