package producto_service.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta paginada")
public record PageResponseDTO<T>(
        @Schema(description = "Elementos de la pagina") List<T> content,
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "51") long totalElements,
        @Schema(example = "3") int totalPages,
        @Schema(example = "true") boolean first,
        @Schema(example = "false") boolean last
) {

    public static <T> PageResponseDTO<T> from(Page<T> pagina) {
        return new PageResponseDTO<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }
}
