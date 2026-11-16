package producto_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para crear o actualizar una categoria")
public class CategoriaRequestDTO {

    @Schema(description = "Nombre de la categoria", example = "Guitarras Acusticas")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @Schema(description = "Descripcion de la categoria", example = "Guitarras de cuerpo hueco y cuerdas de nailon o acero")
    @Size(max = 500, message = "La descripcion no puede superar los 500 caracteres")
    private String descripcion;

    @Schema(description = "Indica si la categoria esta activa", example = "true")
    private Boolean activo;
}
