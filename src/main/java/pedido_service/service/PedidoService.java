package pedido_service.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pedido_service.dto.CrearPedidoRequestDTO;
import pedido_service.dto.DisponibilidadResponseDTO;
import pedido_service.dto.PedidoResponseDTO;
import pedido_service.dto.PedidoResumenDTO;
import pedido_service.dto.ProductoPedidoRequestDTO;
import pedido_service.dto.ProductoResponseDTO;
import pedido_service.dto.ReservaStockRequestDTO;
import pedido_service.dto.ReservaStockResponseDTO;
import pedido_service.exception.PedidoException;
import pedido_service.exception.PedidoNotFoundException;
import pedido_service.mapper.PedidoMapper;
import pedido_service.model.DetallePedido;
import pedido_service.model.EstadoPedido;
import pedido_service.model.EstadoReserva;
import pedido_service.model.Pedido;
import pedido_service.repository.PedidoRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoMapper pedidoMapper;
    private final ProductoServiceClient productoServiceClient;

    public PedidoResponseDTO obtenerPorNumero(
            String numeroPedido,
            String usuarioCorreo) {

        Pedido pedido = obtenerEntidad(numeroPedido, usuarioCorreo);
        return pedidoMapper.toResponseDTO(pedido);
    }

    public List<PedidoResumenDTO> listarPorUsuario(
            String usuarioCorreo) {

        return pedidoRepository
                .findAllByUsuarioCorreoOrderByFechaCreacionDesc(usuarioCorreo)
                .stream()
                .map(pedidoMapper::toResumenDTO)
                .toList();
    }

    @Transactional
    public PedidoResponseDTO crearPedido(
            CrearPedidoRequestDTO solicitud,
            String usuarioCorreo,
            String token) {

        validarSolicitud(solicitud);

        Pedido pedido = construirPedido(solicitud, usuarioCorreo);
        List<String> referenciasReservadas = new ArrayList<>();

        try {
            for (int i = 0; i < solicitud.getProductos().size(); i++) {

                ProductoPedidoRequestDTO producto = solicitud.getProductos().get(i);
                DetallePedido detalle = pedido.getDetalles().get(i);

                ReservaStockResponseDTO reserva =
                        reservarStock(pedido, producto, token);

                if (reserva == null
                        || reserva.getEstado() != EstadoReserva.RESERVADA
                        || reserva.getReferencia() == null
                        || reserva.getReferencia().isBlank()) {

                    throw new PedidoException(
                            "No se pudo reservar el producto: "
                                    + producto.getCodigoProducto());
                }

                detalle.setReferenciaReserva(reserva.getReferencia());
                detalle.setEstadoReserva(EstadoReserva.RESERVADA);

                referenciasReservadas.add(reserva.getReferencia());
            }

        } catch (RuntimeException ex) {

            liberarReservas(referenciasReservadas, token);

            throw new PedidoException(
                    "No fue posible reservar todos los productos del pedido");
        }

        try {
            pedido = pedidoRepository.saveAndFlush(pedido);

        } catch (RuntimeException ex) {

            liberarReservas(referenciasReservadas, token);

            throw new PedidoException(
                    "No fue posible guardar el pedido");
        }

        try {
            confirmarReservas(pedido, token);

            pedido.setEstado(EstadoPedido.CONFIRMADO);
            pedido = pedidoRepository.saveAndFlush(pedido);

        } catch (RuntimeException ex) {
            // El pedido permanece pendiente.
            // Las reservas confirmadas quedan registradas para reintentar
            // sin volver a confirmarlas.
        }

        return pedidoMapper.toResponseDTO(pedido);
    }

    @Transactional
    public PedidoResponseDTO reintentarConfirmacion(
            String numeroPedido,
            String usuarioCorreo,
            String token) {

        Pedido pedido = obtenerEntidad(numeroPedido, usuarioCorreo);

        if (pedido.getEstado() == EstadoPedido.CONFIRMADO) {
            return pedidoMapper.toResponseDTO(pedido);
        }

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new PedidoException(
                    "El pedido no se encuentra pendiente de confirmacion");
        }

        confirmarReservas(pedido, token);

        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedido = pedidoRepository.saveAndFlush(pedido);

        return pedidoMapper.toResponseDTO(pedido);
    }

    @Transactional
    public PedidoResponseDTO cancelarPedido(
            String numeroPedido,
            String usuarioCorreo,
            String token) {

        Pedido pedido = obtenerEntidad(numeroPedido, usuarioCorreo);

        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            return pedidoMapper.toResponseDTO(pedido);
        }

        if (pedido.getEstado() == EstadoPedido.CONFIRMADO) {
            throw new PedidoException(
                    "No se pueden cancelar pedidos confirmados desde este servicio");
        }

        boolean tieneReservasConfirmadas = pedido.getDetalles()
                .stream()
                .anyMatch(detalle ->
                        detalle.getEstadoReserva() == EstadoReserva.CONFIRMADA);

        if (tieneReservasConfirmadas) {
            throw new PedidoException(
                    "No se puede cancelar el pedido porque tiene reservas confirmadas");
        }

        liberarReservasDelPedido(pedido, token);

        pedido.setEstado(EstadoPedido.CANCELADO);
        pedido = pedidoRepository.saveAndFlush(pedido);

        return pedidoMapper.toResponseDTO(pedido);
    }

    private Pedido obtenerEntidad(
            String numeroPedido,
            String usuarioCorreo) {

        return pedidoRepository
                .findByNumeroPedidoAndUsuarioCorreo(
                        numeroPedido,
                        usuarioCorreo)
                .orElseThrow(() -> new PedidoNotFoundException(
                        "No se encontro el pedido solicitado"));
    }

    private void validarSolicitud(CrearPedidoRequestDTO solicitud) {

        if (solicitud == null
                || solicitud.getProductos() == null
                || solicitud.getProductos().isEmpty()) {

            throw new PedidoException(
                    "El pedido debe contener al menos un producto");
        }

        Set<String> codigos = new HashSet<>();

        for (ProductoPedidoRequestDTO producto : solicitud.getProductos()) {

            if (producto == null
                    || producto.getCodigoProducto() == null
                    || producto.getCodigoProducto().isBlank()) {

                throw new PedidoException(
                        "Todos los productos deben tener un codigo valido");
            }

            if (producto.getCantidad() == null
                    || producto.getCantidad() <= 0) {

                throw new PedidoException(
                        "La cantidad de cada producto debe ser mayor que cero");
            }

            String codigoNormalizado = producto.getCodigoProducto()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (!codigos.add(codigoNormalizado)) {
                throw new PedidoException(
                        "No se puede repetir un producto dentro del pedido");
            }
        }
    }

    private Pedido construirPedido(
            CrearPedidoRequestDTO solicitud,
            String usuarioCorreo) {

        Pedido pedido = new Pedido();

        pedido.setNumeroPedido("SV-" + UUID.randomUUID());
        pedido.setUsuarioCorreo(usuarioCorreo);
        pedido.setEstado(EstadoPedido.PENDIENTE);

        BigDecimal total = BigDecimal.ZERO;

        for (ProductoPedidoRequestDTO productoSolicitado
                : solicitud.getProductos()) {

            DetallePedido detalle = construirDetalle(productoSolicitado);

            pedido.agregarDetalle(detalle);
            total = total.add(detalle.getSubtotal());
        }

        pedido.setTotal(total);

        return pedido;
    }

    private DetallePedido construirDetalle(
            ProductoPedidoRequestDTO productoSolicitado) {

        String codigo = productoSolicitado.getCodigoProducto().trim();

        ProductoResponseDTO producto =
                productoServiceClient.obtenerProducto(codigo);

        if (producto == null || !Boolean.TRUE.equals(producto.getActivo())) {
            throw new PedidoException(
                    "El producto solicitado no existe o no esta activo");
        }

        DisponibilidadResponseDTO disponibilidad =
                productoServiceClient.consultarDisponibilidad(codigo);

        if (disponibilidad == null
                || !Boolean.TRUE.equals(disponibilidad.getDisponible())
                || disponibilidad.getStockDisponible() == null
                || disponibilidad.getStockDisponible()
                        < productoSolicitado.getCantidad()) {

            throw new PedidoException(
                    "No hay stock suficiente para el producto: " + codigo);
        }

        BigDecimal precio = producto.getPrecioEfectivo();

        if (precio == null || precio.signum() <= 0) {
            throw new PedidoException(
                    "El producto no tiene un precio valido");
        }

        DetallePedido detalle = new DetallePedido();

        detalle.setCodigoProducto(producto.getCodigo());
        detalle.setNombreProducto(producto.getNombre());
        detalle.setPrecioUnitario(precio);
        detalle.setCantidad(productoSolicitado.getCantidad());

        detalle.setSubtotal(
                precio.multiply(
                        BigDecimal.valueOf(productoSolicitado.getCantidad())));

        return detalle;
    }

    private ReservaStockResponseDTO reservarStock(
            Pedido pedido,
            ProductoPedidoRequestDTO productoSolicitado,
            String token) {

        String codigo = productoSolicitado.getCodigoProducto().trim();

        ReservaStockRequestDTO solicitud = new ReservaStockRequestDTO();

        solicitud.setPedidoId(pedido.getNumeroPedido());
        solicitud.setCodigoProducto(codigo);
        solicitud.setCantidad(productoSolicitado.getCantidad());

        solicitud.setReferencia(
                pedido.getNumeroPedido() + "-" + codigo);

        return productoServiceClient.reservarStock(solicitud, token);
    }

    private void confirmarReservas(Pedido pedido, String token) {

        for (DetallePedido detalle : pedido.getDetalles()) {

            if (detalle.getEstadoReserva() == EstadoReserva.CONFIRMADA) {
                continue;
            }

            if (detalle.getEstadoReserva() != EstadoReserva.RESERVADA
                    || detalle.getReferenciaReserva() == null) {

                throw new PedidoException(
                        "El detalle no tiene una reserva valida: "
                                + detalle.getCodigoProducto());
            }

            ReservaStockResponseDTO confirmacion =
                    productoServiceClient.confirmarReserva(
                            detalle.getReferenciaReserva(),
                            token);

            if (confirmacion == null
                    || confirmacion.getEstado() != EstadoReserva.CONFIRMADA) {

                throw new PedidoException(
                        "No se pudo confirmar la reserva: "
                                + detalle.getReferenciaReserva());
            }

            detalle.setEstadoReserva(EstadoReserva.CONFIRMADA);
        }
    }

    private void liberarReservasDelPedido(
            Pedido pedido,
            String token) {

        for (DetallePedido detalle : pedido.getDetalles()) {

            if (detalle.getEstadoReserva() == EstadoReserva.LIBERADA) {
                continue;
            }

            if (detalle.getEstadoReserva() == null) {
                continue;
            }

            if (detalle.getEstadoReserva() == EstadoReserva.CONFIRMADA) {
                throw new PedidoException(
                        "No se puede liberar una reserva confirmada");
            }

            if (detalle.getReferenciaReserva() == null) {
                throw new PedidoException(
                        "No se encontro la referencia de una reserva");
            }

            ReservaStockResponseDTO liberacion =
                    productoServiceClient.liberarReserva(
                            detalle.getReferenciaReserva(),
                            token);

            if (liberacion == null
                    || liberacion.getEstado() != EstadoReserva.LIBERADA) {

                throw new PedidoException(
                        "No se pudo liberar la reserva: "
                                + detalle.getReferenciaReserva());
            }

            detalle.setEstadoReserva(EstadoReserva.LIBERADA);
        }
    }

    private void liberarReservas(
            List<String> referencias,
            String token) {

        for (String referencia : referencias) {
            try {
                productoServiceClient.liberarReserva(referencia, token);
            } catch (RuntimeException ex) {
                // Se intenta liberar el resto de las reservas.
            }
        }
    }
}
