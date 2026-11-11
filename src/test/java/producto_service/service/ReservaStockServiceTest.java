package producto_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import producto_service.dto.ReservaStockRequestDTO;
import producto_service.dto.ReservaStockResponseDTO;
import producto_service.exception.ConflictException;
import producto_service.exception.ResourceNotFoundException;
import producto_service.model.Categoria;
import producto_service.model.EstadoReserva;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.model.ReservaStock;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;
import producto_service.repository.ReservaStockRepository;
import producto_service.service.ReservaStockService.ResultadoReserva;

@ExtendWith(MockitoExtension.class)
class ReservaStockServiceTest {

    @Mock
    private ReservaStockRepository reservaStockRepository;

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    private ReservaStockService reservaStockService;

    @BeforeEach
    void setUp() {
        reservaStockService = new ReservaStockService(
                reservaStockRepository,
                inventarioRepository,
                productoRepository
        );
    }

    @Test
    @DisplayName("reservar stock incrementa stockReservado y crea la reserva")
    void reservarStockCorrectamente() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 10, 0);

        when(productoRepository.findByCodigoAndActivoTrue("GA001")).thenReturn(Optional.of(producto));
        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));
        when(reservaStockRepository.findByReferencia("REF-1")).thenReturn(Optional.empty());
        when(inventarioRepository.saveAndFlush(any(Inventario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(reservaStockRepository.saveAndFlush(any(ReservaStock.class))).thenAnswer(invocacion -> {
            ReservaStock reserva = invocacion.getArgument(0);
            reserva.setId(1L);
            return reserva;
        });

        ResultadoReserva resultado = reservaStockService.reservar(request("PED-1", "ga001", 3, "REF-1"));

        assertTrue(resultado.creada());
        assertEquals(EstadoReserva.RESERVADA, resultado.reserva().getEstado());
        assertEquals(3, resultado.reserva().getCantidad());
        assertEquals(7, resultado.reserva().getStockDisponible());
        assertEquals(3, inventario.getStockReservado());
    }

    @Test
    @DisplayName("no reservar mas unidades que el stock disponible")
    void reservarSinStockSuficiente() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 2, 1);

        when(productoRepository.findByCodigoAndActivoTrue("GA001")).thenReturn(Optional.of(producto));
        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));
        when(reservaStockRepository.findByReferencia("REF-1")).thenReturn(Optional.empty());

        assertThrows(
                ConflictException.class,
                () -> reservaStockService.reservar(request("PED-1", "GA001", 2, "REF-1"))
        );

        verify(inventarioRepository, never()).saveAndFlush(any(Inventario.class));
        verify(reservaStockRepository, never()).saveAndFlush(any(ReservaStock.class));
    }

    @Test
    @DisplayName("reintento con la misma referencia es idempotente")
    void reservaDuplicadaMismaReferenciaEsIdempotente() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 10, 3);
        ReservaStock existente = reserva(producto, "PED-1", 3, "REF-1", EstadoReserva.RESERVADA);

        when(productoRepository.findByCodigoAndActivoTrue("GA001")).thenReturn(Optional.of(producto));
        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));
        when(reservaStockRepository.findByReferencia("REF-1")).thenReturn(Optional.of(existente));

        ResultadoReserva resultado = reservaStockService.reservar(request("PED-1", "GA001", 3, "REF-1"));

        assertFalse(resultado.creada());
        assertEquals(3, inventario.getStockReservado());
        verify(inventarioRepository, never()).saveAndFlush(any(Inventario.class));
        verify(reservaStockRepository, never()).saveAndFlush(any(ReservaStock.class));
    }

    @Test
    @DisplayName("referencia reutilizada con otros datos retorna conflicto")
    void referenciaReutilizadaConOtrosDatos() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 10, 3);
        ReservaStock existente = reserva(producto, "PED-1", 3, "REF-1", EstadoReserva.RESERVADA);

        when(productoRepository.findByCodigoAndActivoTrue("GA001")).thenReturn(Optional.of(producto));
        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));
        when(reservaStockRepository.findByReferencia("REF-1")).thenReturn(Optional.of(existente));

        assertThrows(
                ConflictException.class,
                () -> reservaStockService.reservar(request("PED-1", "GA001", 5, "REF-1"))
        );
    }

    @Test
    @DisplayName("confirmar reserva descuenta stock fisico y reservado")
    void confirmarReserva() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 10, 3);
        ReservaStock reserva = reserva(producto, "PED-1", 3, "REF-1", EstadoReserva.RESERVADA);

        when(reservaStockRepository.findByReferenciaForUpdate("REF-1")).thenReturn(Optional.of(reserva));
        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));
        when(inventarioRepository.saveAndFlush(any(Inventario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(reservaStockRepository.saveAndFlush(any(ReservaStock.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ReservaStockResponseDTO resultado = reservaStockService.confirmar("REF-1");

        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
        assertEquals(7, inventario.getStockFisico());
        assertEquals(0, inventario.getStockReservado());
        assertEquals(7, resultado.getStockDisponible());
    }

    @Test
    @DisplayName("confirmar una reserva ya confirmada es idempotente")
    void confirmarReservaYaConfirmada() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 7, 0);
        ReservaStock reserva = reserva(producto, "PED-1", 3, "REF-1", EstadoReserva.CONFIRMADA);

        when(reservaStockRepository.findByReferenciaForUpdate("REF-1")).thenReturn(Optional.of(reserva));
        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));

        ReservaStockResponseDTO resultado = reservaStockService.confirmar("REF-1");

        assertEquals(EstadoReserva.CONFIRMADA, resultado.getEstado());
        assertEquals(7, inventario.getStockFisico());
        verify(inventarioRepository, never()).saveAndFlush(any(Inventario.class));
        verify(reservaStockRepository, never()).saveAndFlush(any(ReservaStock.class));
    }

    @Test
    @DisplayName("liberar reserva devuelve unidades sin tocar stock fisico")
    void liberarReserva() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 10, 3);
        ReservaStock reserva = reserva(producto, "PED-1", 3, "REF-1", EstadoReserva.RESERVADA);

        when(reservaStockRepository.findByReferenciaForUpdate("REF-1")).thenReturn(Optional.of(reserva));
        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));
        when(inventarioRepository.saveAndFlush(any(Inventario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(reservaStockRepository.saveAndFlush(any(ReservaStock.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ReservaStockResponseDTO resultado = reservaStockService.liberar("REF-1");

        assertEquals(EstadoReserva.LIBERADA, resultado.getEstado());
        assertEquals(10, inventario.getStockFisico());
        assertEquals(0, inventario.getStockReservado());
        assertEquals(10, resultado.getStockDisponible());
    }

    @Test
    @DisplayName("no liberar una reserva confirmada")
    void noLiberarReservaConfirmada() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 7, 0);
        ReservaStock reserva = reserva(producto, "PED-1", 3, "REF-1", EstadoReserva.CONFIRMADA);

        when(reservaStockRepository.findByReferenciaForUpdate("REF-1")).thenReturn(Optional.of(reserva));
        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));

        assertThrows(ConflictException.class, () -> reservaStockService.liberar("REF-1"));
        verify(inventarioRepository, never()).saveAndFlush(any(Inventario.class));
    }

    @Test
    @DisplayName("no reservar producto inactivo o inexistente")
    void reservarProductoInactivo() {
        when(productoRepository.findByCodigoAndActivoTrue("GA001")).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> reservaStockService.reservar(request("PED-1", "GA001", 1, "REF-1"))
        );
    }

    @Test
    @DisplayName("listar reservas de un pedido")
    void listarReservasPorPedido() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 10, 3);
        ReservaStock reserva = reserva(producto, "PED-1", 3, "REF-1", EstadoReserva.RESERVADA);

        when(reservaStockRepository.findByPedidoIdOrderByFechaCreacionAsc("PED-1"))
                .thenReturn(List.of(reserva));
        when(inventarioRepository.findByProductoId(1L)).thenReturn(Optional.of(inventario));

        List<ReservaStockResponseDTO> resultado = reservaStockService.listarPorPedido("PED-1");

        assertEquals(1, resultado.size());
        assertEquals("REF-1", resultado.get(0).getReferencia());
        assertEquals(7, resultado.get(0).getStockDisponible());
    }

    private ReservaStockRequestDTO request(String pedidoId, String codigo, int cantidad, String referencia) {
        ReservaStockRequestDTO dto = new ReservaStockRequestDTO();
        dto.setPedidoId(pedidoId);
        dto.setCodigoProducto(codigo);
        dto.setCantidad(cantidad);
        dto.setReferencia(referencia);
        return dto;
    }

    private Producto producto(String codigo) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Guitarras Acusticas");
        categoria.setActivo(true);

        Producto producto = new Producto();
        producto.setId(1L);
        producto.setCodigo(codigo);
        producto.setNombre("Guitarra");
        producto.setPrecio(new BigDecimal("1000.00"));
        producto.setEnOferta(false);
        producto.setActivo(true);
        producto.setCategoria(categoria);
        return producto;
    }

    private Inventario inventario(Producto producto, int fisico, int reservado) {
        Inventario inventario = new Inventario();
        inventario.setId(1L);
        inventario.setProducto(producto);
        inventario.setStockFisico(fisico);
        inventario.setStockReservado(reservado);
        inventario.setStockCritico(3);
        producto.setInventario(inventario);
        return inventario;
    }

    private ReservaStock reserva(
            Producto producto,
            String pedidoId,
            int cantidad,
            String referencia,
            EstadoReserva estado
    ) {
        ReservaStock reserva = new ReservaStock();
        reserva.setId(1L);
        reserva.setProducto(producto);
        reserva.setPedidoId(pedidoId);
        reserva.setCantidad(cantidad);
        reserva.setReferencia(referencia);
        reserva.setEstado(estado);
        return reserva;
    }
}
