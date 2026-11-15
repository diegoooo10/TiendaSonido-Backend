package pedido_service.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductoResponseDTO {

    private Long id;

    private String codigo;

    private String nombre;

    private BigDecimal precioEfectivo;

    private Boolean activo;

    private Integer stockDisponible;

    private Boolean disponible;
}
