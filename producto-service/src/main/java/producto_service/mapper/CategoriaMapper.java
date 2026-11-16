package producto_service.mapper;

import producto_service.dto.CategoriaRequestDTO;
import producto_service.dto.CategoriaResponseDTO;
import producto_service.model.Categoria;

public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    public static Categoria toEntity(CategoriaRequestDTO dto) {
        Categoria categoria = new Categoria();
        aplicar(dto, categoria);
        return categoria;
    }

    public static void aplicar(CategoriaRequestDTO dto, Categoria categoria) {
        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());
    }

    public static CategoriaResponseDTO toResponse(Categoria categoria) {
        return CategoriaResponseDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .activo(Boolean.TRUE.equals(categoria.getActivo()))
                .build();
    }
}
