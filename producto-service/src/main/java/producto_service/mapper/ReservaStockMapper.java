package producto_service.mapper;

import producto_service.dto.ReservaStockResponseDTO;
import producto_service.model.ReservaStock;

public final class ReservaStockMapper {

    private ReservaStockMapper() {
    }

    public static ReservaStockResponseDTO toResponse(ReservaStock reserva, int stockDisponible) {
        return ReservaStockResponseDTO.builder()
                .id(reserva.getId())
                .referencia(reserva.getReferencia())
                .pedidoId(reserva.getPedidoId())
                .codigoProducto(reserva.getProducto() == null ? null : reserva.getProducto().getCodigo())
                .cantidad(reserva.getCantidad())
                .estado(reserva.getEstado())
                .stockDisponible(stockDisponible)
                .fechaCreacion(reserva.getFechaCreacion())
                .fechaActualizacion(reserva.getFechaActualizacion())
                .build();
    }
}
