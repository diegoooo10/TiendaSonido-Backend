package producto_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de error de la API")
public record ErrorResponseDTO(
        @Schema(example = "2026-11-10T15:30:00") LocalDateTime timestamp,
        @Schema(example = "404") int codigo,
        @Schema(example = "No encontrado") String error,
        @Schema(example = "Producto no encontrado") String mensaje,
        @Schema(example = "/api/productos/XX999") String path,
        @Schema(description = "Errores de validacion, si aplica") List<String> errores
) {
}
