package producto_service.mapper;

import producto_service.dto.InventarioResponseDTO;
import producto_service.model.Inventario;
import producto_service.model.Producto;

public final class InventarioMapper {

    private InventarioMapper() {
    }

    public static InventarioResponseDTO toResponse(Inventario inventario) {
        Producto producto = inventario.getProducto();
        int disponible = inventario.getStockDisponible();

        return InventarioResponseDTO.builder()
                .id(inventario.getId())
                .productoId(producto == null ? null : producto.getId())
                .codigoProducto(producto == null ? null : producto.getCodigo())
                .nombreProducto(producto == null ? null : producto.getNombre())
                .stockFisico(inventario.getStockFisico())
                .stockReservado(inventario.getStockReservado())
                .stockDisponible(disponible)
                .stockCritico(inventario.getStockCritico())
                .bajoStockCritico(inventario.isBajoStockCritico())
                .fechaActualizacion(inventario.getFechaActualizacion())
                .build();
    }
}
