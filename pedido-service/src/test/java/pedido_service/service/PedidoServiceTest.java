package pedido_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pedido_service.dto.CrearPedidoRequestDTO;
import pedido_service.dto.DisponibilidadResponseDTO;
import pedido_service.dto.PedidoResponseDTO;
import pedido_service.dto.ProductoPedidoRequestDTO;
import pedido_service.dto.ProductoResponseDTO;
import pedido_service.dto.ReservaStockResponseDTO;
import pedido_service.exception.PedidoException;
import pedido_service.exception.PedidoNotFoundException;
import pedido_service.mapper.PedidoMapper;
import pedido_service.model.DetallePedido;
import pedido_service.model.EstadoPedido;
import pedido_service.model.EstadoReserva;
import pedido_service.model.Pedido;
import pedido_service.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoServiceClient productoServiceClient;

    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService(
                pedidoRepository,
                new PedidoMapper(),
                productoServiceClient
        );
    }

    @Test
    @DisplayName("crear pedido correctamente y confirmar sus reservas")
    void crearPedidoCorrectamente() {
        CrearPedidoRequestDTO solicitud = solicitudPedido();

        when(productoServiceClient.obtenerProducto("GA001"))
                .thenReturn(producto());

        when(productoServiceClient.consultarDisponibilidad("GA001"))
                .thenReturn(disponibilidad());

        when(productoServiceClient.reservarStock(any(), anyString()))
                .thenReturn(reserva(EstadoReserva.RESERVADA));

        when(productoServiceClient.confirmarReserva(anyString(), anyString()))
                .thenReturn(reserva(EstadoReserva.CONFIRMADA));

        when(pedidoRepository.saveAndFlush(any(Pedido.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.crearPedido(
                solicitud,
                "cliente@tienda.cl",
                "token-prueba"
        );

        assertNotNull(resultado);
        assertNotNull(resultado.getNumeroPedido());
        assertEquals("cliente@tienda.cl", resultado.getUsuarioCorreo());
        assertEquals(EstadoPedido.CONFIRMADO, resultado.getEstado());
        assertEquals(1, resultado.getProductos().size());
        assertEquals(0, new BigDecimal("129990.00").compareTo(resultado.getTotal()));

        verify(productoServiceClient).reservarStock(any(), anyString());
        verify(productoServiceClient).confirmarReserva(anyString(), anyString());
    }

    @Test
    @DisplayName("crear pedido rechaza productos sin stock suficiente")
    void crearPedidoSinStockSuficiente() {
        CrearPedidoRequestDTO solicitud = solicitudPedido();

        DisponibilidadResponseDTO sinStock = new DisponibilidadResponseDTO();
        sinStock.setCodigoProducto("GA001");
        sinStock.setNombreProducto("Guitarra Acustica");
        sinStock.setStockDisponible(0);
        sinStock.setDisponible(false);

        when(productoServiceClient.obtenerProducto("GA001"))
                .thenReturn(producto());

        when(productoServiceClient.consultarDisponibilidad("GA001"))
                .thenReturn(sinStock);

        assertThrows(
                PedidoException.class,
                () -> pedidoService.crearPedido(
                        solicitud,
                        "cliente@tienda.cl",
                        "token-prueba"
                )
        );

        verify(productoServiceClient, never())
                .reservarStock(any(), anyString());

        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("crear pedido rechaza productos repetidos")
    void crearPedidoConProductosRepetidos() {
        CrearPedidoRequestDTO solicitud = solicitudPedido();

        solicitud.getProductos().add(productoSolicitado());

        assertThrows(
                PedidoException.class,
                () -> pedidoService.crearPedido(
                        solicitud,
                        "cliente@tienda.cl",
                        "token-prueba"
                )
        );

        verify(productoServiceClient, never()).obtenerProducto(anyString());
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("obtener pedido inexistente lanza excepcion")
    void obtenerPedidoInexistente() {
        when(pedidoRepository.findByNumeroPedidoAndUsuarioCorreo(
                "SV-INEXISTENTE",
                "cliente@tienda.cl"
        )).thenReturn(Optional.empty());

        assertThrows(
                PedidoNotFoundException.class,
                () -> pedidoService.obtenerPorNumero(
                        "SV-INEXISTENTE",
                        "cliente@tienda.cl"
                )
        );
    }

    @Test
    @DisplayName("reintentar confirmacion actualiza el estado del pedido")
    void reintentarConfirmacionCorrectamente() {
        Pedido pedido = new Pedido();
        pedido.setNumeroPedido("SV-PRUEBA");
        pedido.setUsuarioCorreo("cliente@tienda.cl");
        pedido.setTotal(new BigDecimal("129990.00"));
        pedido.setEstado(EstadoPedido.PENDIENTE);

        DetallePedido detalle = new DetallePedido();
        detalle.setCodigoProducto("GA001");
        detalle.setNombreProducto("Guitarra Acustica");
        detalle.setPrecioUnitario(new BigDecimal("129990.00"));
        detalle.setCantidad(1);
        detalle.setSubtotal(new BigDecimal("129990.00"));
        detalle.setEstadoReserva(EstadoReserva.RESERVADA);
        detalle.setReferenciaReserva("SV-PRUEBA-GA001");

        pedido.agregarDetalle(detalle);

        when(pedidoRepository.findByNumeroPedidoAndUsuarioCorreo(
                "SV-PRUEBA",
                "cliente@tienda.cl"
        )).thenReturn(Optional.of(pedido));

        when(productoServiceClient.confirmarReserva(
                "SV-PRUEBA-GA001",
                "token-prueba"
        )).thenReturn(reserva(EstadoReserva.CONFIRMADA));

        when(pedidoRepository.saveAndFlush(any(Pedido.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        PedidoResponseDTO resultado = pedidoService.reintentarConfirmacion(
                "SV-PRUEBA",
                "cliente@tienda.cl",
                "token-prueba"
        );

        assertEquals(EstadoPedido.CONFIRMADO, resultado.getEstado());
        verify(productoServiceClient).confirmarReserva(
                "SV-PRUEBA-GA001",
                "token-prueba"
        );
    }

    private CrearPedidoRequestDTO solicitudPedido() {
        CrearPedidoRequestDTO solicitud = new CrearPedidoRequestDTO();
        solicitud.setProductos(new ArrayList<>(List.of(productoSolicitado())));
        return solicitud;
    }

    private ProductoPedidoRequestDTO productoSolicitado() {
        ProductoPedidoRequestDTO producto = new ProductoPedidoRequestDTO();
        producto.setCodigoProducto("GA001");
        producto.setCantidad(1);
        return producto;
    }

    private ProductoResponseDTO producto() {
        ProductoResponseDTO producto = new ProductoResponseDTO();
        producto.setCodigo("GA001");
        producto.setNombre("Guitarra Acustica");
        producto.setPrecioEfectivo(new BigDecimal("129990.00"));
        producto.setActivo(true);
        return producto;
    }

    private DisponibilidadResponseDTO disponibilidad() {
        DisponibilidadResponseDTO disponibilidad =
                new DisponibilidadResponseDTO();

        disponibilidad.setCodigoProducto("GA001");
        disponibilidad.setNombreProducto("Guitarra Acustica");
        disponibilidad.setStockDisponible(5);
        disponibilidad.setDisponible(true);

        return disponibilidad;
    }

    private ReservaStockResponseDTO reserva(EstadoReserva estado) {
        ReservaStockResponseDTO reserva = new ReservaStockResponseDTO();
        reserva.setReferencia("SV-PRUEBA-GA001");
        reserva.setEstado(estado);
        return reserva;
    }
}
