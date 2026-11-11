package producto_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Disponibilidad publica de un producto")
public class DisponibilidadResponseDTO {

    @Schema(example = "GA001")
    private String codigoProducto;

    @Schema(example = "Guitarra Acustica Folk")
    private String nombreProducto;

    @Schema(example = "6")
    private Integer stockDisponible;

    @Schema(example = "true")
    private Boolean disponible;
}
