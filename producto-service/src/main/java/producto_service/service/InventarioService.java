package producto_service.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import producto_service.dto.ActualizarStockRequestDTO;
import producto_service.dto.DisponibilidadResponseDTO;
import producto_service.dto.InventarioResponseDTO;
import producto_service.exception.BusinessException;
import producto_service.exception.ConflictException;
import producto_service.exception.ResourceNotFoundException;
import producto_service.mapper.InventarioMapper;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<InventarioResponseDTO> listar() {
        log.info("Listando inventario");
        return inventarioRepository.findAllConProducto()
                .stream()
                .map(InventarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventarioResponseDTO obtenerPorCodigo(String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);
        log.info("Buscando inventario del producto {}", codigoNormalizado);

        Producto producto = productoRepository.findByCodigo(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        Inventario inventario = inventarioRepository.findByProductoId(producto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventario no encontrado para el producto"));

        return InventarioMapper.toResponse(inventario);
    }

    /**
     * Consulta publica: solo revela disponibilidad de productos activos.
     */
    @Transactional(readOnly = true)
    public DisponibilidadResponseDTO consultarDisponibilidad(String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);

        Producto producto = productoRepository.findByCodigoAndActivoTrue(codigoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        int disponible = inventarioRepository.findByProductoId(producto.getId())
                .map(Inventario::getStockDisponible)
                .orElse(0);

        return DisponibilidadResponseDTO.builder()
                .codigoProducto(producto.getCodigo())
                .nombreProducto(producto.getNombre())
                .stockDisponible(disponible)
                .disponible(disponible > 0)
                .build();
    }

    @Transactional
    public InventarioResponseDTO actualizarStock(String codigo, ActualizarStockRequestDTO dto) {
        String codigoNormalizado = normalizarCodigo(codigo);
        log.info("Actualizando stock fisico del producto {}", codigoNormalizado);

        Inventario inventario = inventarioRepository.findByProductoCodigoForUpdate(codigoNormalizado)
                .orElseGet(() -> {
                    Producto producto = productoRepository.findByCodigo(codigoNormalizado)
                            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

                    Inventario nuevo = new Inventario();
                    nuevo.setProducto(producto);
                    nuevo.setStockFisico(0);
                    nuevo.setStockReservado(0);
                    return nuevo;
                });

        if (dto.getStockFisico() < inventario.getStockReservado()) {
            throw new ConflictException(
                    "El stock fisico no puede ser menor al stock reservado (" + inventario.getStockReservado() + ")"
            );
        }

        if (dto.getStockFisico() < 0) {
            throw new BusinessException("El stock fisico no puede ser negativo");
        }

        inventario.setStockFisico(dto.getStockFisico());
        if (dto.getStockCritico() != null) {
            inventario.setStockCritico(dto.getStockCritico());
        }

        Inventario actualizado = inventarioRepository.saveAndFlush(inventario);
        log.info("Stock del producto {} actualizado a {}", codigoNormalizado, actualizado.getStockFisico());
        return InventarioMapper.toResponse(actualizado);
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El codigo del producto es obligatorio");
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
}
