package producto_service.dto;

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
@Schema(description = "Respuesta de una categoria")
public class CategoriaResponseDTO extends RepresentationModel<CategoriaResponseDTO> {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "Guitarras Acusticas")
    private String nombre;

    @Schema(example = "Guitarras de cuerpo hueco y cuerdas de nailon o acero")
    private String descripcion;

    @Schema(example = "true")
    private Boolean activo;
}
