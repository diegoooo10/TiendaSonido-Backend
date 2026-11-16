package pedido_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DisponibilidadResponseDTO {

    private String codigoProducto;

    private String nombreProducto;

    private Integer stockDisponible;

    private Boolean disponible;
}
