package producto_service.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import producto_service.dto.ReservaStockRequestDTO;
import producto_service.dto.ReservaStockResponseDTO;
import producto_service.exception.BusinessException;
import producto_service.exception.ConflictException;
import producto_service.exception.ResourceNotFoundException;
import producto_service.mapper.ReservaStockMapper;
import producto_service.model.EstadoReserva;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.model.ReservaStock;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;
import producto_service.repository.ReservaStockRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservaStockService {

    private final ReservaStockRepository reservaStockRepository;
    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;

    /**
     * Resultado interno para distinguir una reserva nueva de un reintento
     * idempotente y responder 201 o 200 segun corresponda.
     */
    public record ResultadoReserva(ReservaStockResponseDTO reserva, boolean creada) {
    }

    @Transactional
    public ResultadoReserva reservar(ReservaStockRequestDTO dto) {
        String codigo = normalizarCodigo(dto.getCodigoProducto());
        String referencia = normalizarTexto(dto.getReferencia(), "referencia");
        String pedidoId = normalizarTexto(dto.getPedidoId(), "pedidoId");
        int cantidad = validarCantidad(dto.getCantidad());

        log.info("Reservando {} unidades del producto {} para el pedido {} (ref {})",
                cantidad, codigo, pedidoId, referencia);

        // Comprobacion sin cargar entidades: evita dejar una version obsoleta de
        // Inventario en el contexto antes de tomar el bloqueo pesimista.
        if (!productoRepository.existsByCodigoAndActivoTrue(codigo)) {
            throw new ResourceNotFoundException("Producto no encontrado o inactivo");
        }

        Inventario inventario = inventarioRepository.findByProductoCodigoForUpdate(codigo)
                .orElseThrow(() -> new ConflictException("El producto no tiene inventario asociado"));

        Producto producto = inventario.getProducto();
        if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) {
            throw new ResourceNotFoundException("Producto no encontrado o inactivo");
        }

        // Lectura con bloqueo (current read): si otra transaccion creo la misma
        // referencia mientras esperabamos el bloqueo del inventario, aqui se ve
        // el valor confirmado y la operacion sigue siendo idempotente.
        ReservaStock existente = reservaStockRepository.findByReferenciaForUpdate(referencia).orElse(null);
        if (existente != null) {
            validarMismaReserva(existente, producto, pedidoId, cantidad);
            log.info("Reserva {} ya existia; se devuelve sin modificar stock", referencia);
            return new ResultadoReserva(
                    ReservaStockMapper.toResponse(existente, inventario.getStockDisponible()),
                    false
            );
        }

        if (cantidad > inventario.getStockDisponible()) {
            throw new ConflictException(
                    "Stock insuficiente. Disponible: " + inventario.getStockDisponible() + ", solicitado: " + cantidad
            );
        }

        inventario.setStockReservado(inventario.getStockReservado() + cantidad);
        inventarioRepository.saveAndFlush(inventario);

        ReservaStock reserva = new ReservaStock();
        reserva.setReferencia(referencia);
        reserva.setPedidoId(pedidoId);
        reserva.setProducto(producto);
        reserva.setCantidad(cantidad);
        reserva.setEstado(EstadoReserva.RESERVADA);

        ReservaStock guardada = reservaStockRepository.saveAndFlush(reserva);

        log.info("Reserva {} creada para el producto {}", referencia, codigo);
        return new ResultadoReserva(
                ReservaStockMapper.toResponse(guardada, inventario.getStockDisponible()),
                true
        );
    }

    /**
     * Confirma una reserva: descuenta el stock fisico y libera el reservado.
     * Es idempotente si la reserva ya estaba confirmada.
     */
    @Transactional
    public ReservaStockResponseDTO confirmar(String referencia) {
        String referenciaNormalizada = normalizarTexto(referencia, "referencia");
        log.info("Confirmando reserva {}", referenciaNormalizada);

        ReservaStock reserva = reservaStockRepository.findByReferenciaForUpdate(referenciaNormalizada)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        Inventario inventario = bloquearInventario(reserva);

        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            log.info("Reserva {} ya estaba confirmada", referenciaNormalizada);
            return ReservaStockMapper.toResponse(reserva, inventario.getStockDisponible());
        }

        if (reserva.getEstado() == EstadoReserva.LIBERADA) {
            throw new ConflictException("Una reserva liberada no puede confirmarse");
        }

        if (inventario.getStockFisico() < reserva.getCantidad()
                || inventario.getStockReservado() < reserva.getCantidad()) {
            throw new ConflictException("Inconsistencia de inventario al confirmar la reserva");
        }

        inventario.setStockFisico(inventario.getStockFisico() - reserva.getCantidad());
        inventario.setStockReservado(inventario.getStockReservado() - reserva.getCantidad());
        inventarioRepository.saveAndFlush(inventario);

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        ReservaStock confirmada = reservaStockRepository.saveAndFlush(reserva);

        log.info("Reserva {} confirmada", referenciaNormalizada);
        return ReservaStockMapper.toResponse(confirmada, inventario.getStockDisponible());
    }

    /**
     * Libera una reserva activa sin descontar stock fisico.
     * Es idempotente si la reserva ya estaba liberada.
     */
    @Transactional
    public ReservaStockResponseDTO liberar(String referencia) {
        String referenciaNormalizada = normalizarTexto(referencia, "referencia");
        log.info("Liberando reserva {}", referenciaNormalizada);

        ReservaStock reserva = reservaStockRepository.findByReferenciaForUpdate(referenciaNormalizada)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        Inventario inventario = bloquearInventario(reserva);

        if (reserva.getEstado() == EstadoReserva.LIBERADA) {
            log.info("Reserva {} ya estaba liberada", referenciaNormalizada);
            return ReservaStockMapper.toResponse(reserva, inventario.getStockDisponible());
        }

        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            throw new ConflictException("Una reserva confirmada no puede liberarse");
        }

        if (inventario.getStockReservado() < reserva.getCantidad()) {
            throw new ConflictException("Inconsistencia de inventario al liberar la reserva");
        }

        inventario.setStockReservado(inventario.getStockReservado() - reserva.getCantidad());
        inventarioRepository.saveAndFlush(inventario);

        reserva.setEstado(EstadoReserva.LIBERADA);
        ReservaStock liberada = reservaStockRepository.saveAndFlush(reserva);

        log.info("Reserva {} liberada", referenciaNormalizada);
        return ReservaStockMapper.toResponse(liberada, inventario.getStockDisponible());
    }

    @Transactional(readOnly = true)
    public ReservaStockResponseDTO obtener(String referencia) {
        String referenciaNormalizada = normalizarTexto(referencia, "referencia");

        ReservaStock reserva = reservaStockRepository.findByReferencia(referenciaNormalizada)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        return ReservaStockMapper.toResponse(reserva, stockDisponible(reserva));
    }

    @Transactional(readOnly = true)
    public List<ReservaStockResponseDTO> listarPorPedido(String pedidoId) {
        String pedidoNormalizado = normalizarTexto(pedidoId, "pedidoId");

        return reservaStockRepository.findByPedidoIdOrderByFechaCreacionAsc(pedidoNormalizado)
                .stream()
                .map(reserva -> ReservaStockMapper.toResponse(reserva, stockDisponible(reserva)))
                .toList();
    }

    private Inventario bloquearInventario(ReservaStock reserva) {
        String codigo = reserva.getProducto().getCodigo();
        return inventarioRepository.findByProductoCodigoForUpdate(codigo)
                .orElseThrow(() -> new ConflictException("El producto no tiene inventario asociado"));
    }

    private int stockDisponible(ReservaStock reserva) {
        return inventarioRepository.findByProductoId(reserva.getProducto().getId())
                .map(Inventario::getStockDisponible)
                .orElse(0);
    }

    private void validarMismaReserva(ReservaStock existente, Producto producto, String pedidoId, int cantidad) {
        boolean mismoProducto = existente.getProducto() != null
                && existente.getProducto().getCodigo().equalsIgnoreCase(producto.getCodigo());
        boolean mismoPedido = existente.getPedidoId().equals(pedidoId);
        boolean mismaCantidad = existente.getCantidad() == cantidad;

        if (!mismoProducto || !mismoPedido || !mismaCantidad) {
            throw new ConflictException("La referencia ya fue utilizada con otros datos de reserva");
        }
    }

    private int validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad < 1) {
            throw new BusinessException("La cantidad debe ser al menos 1");
        }
        return cantidad;
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException("El codigo del producto es obligatorio");
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessException("El " + campo + " es obligatorio");
        }
        return valor.trim();
    }
}
