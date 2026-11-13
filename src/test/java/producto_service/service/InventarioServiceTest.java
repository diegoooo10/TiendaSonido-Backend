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

import producto_service.dto.ActualizarStockRequestDTO;
import producto_service.dto.DisponibilidadResponseDTO;
import producto_service.dto.InventarioResponseDTO;
import producto_service.exception.ConflictException;
import producto_service.exception.ResourceNotFoundException;
import producto_service.model.Categoria;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private InventarioRepository inventarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    private InventarioService inventarioService;

    @BeforeEach
    void setUp() {
        inventarioService = new InventarioService(inventarioRepository, productoRepository);
    }

    @Test
    @DisplayName("calcular stock disponible como fisico menos reservado")
    void calcularStockDisponible() {
        Inventario inventario = inventario(8, 3);

        assertEquals(5, inventario.getStockDisponible());
        assertFalse(inventario.isBajoStockCritico());
    }

    @Test
    @DisplayName("stock disponible nunca es negativo")
    void stockDisponibleNoEsNegativo() {
        Inventario inventario = inventario(2, 5);

        assertEquals(0, inventario.getStockDisponible());
    }

    @Test
    @DisplayName("consultar disponibilidad publica de producto activo")
    void consultarDisponibilidad() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 8, 2);

        when(productoRepository.findByCodigoAndActivoTrue("GA001")).thenReturn(Optional.of(producto));
        when(inventarioRepository.findByProductoId(1L)).thenReturn(Optional.of(inventario));

        DisponibilidadResponseDTO resultado = inventarioService.consultarDisponibilidad("ga001");

        assertEquals("GA001", resultado.getCodigoProducto());
        assertEquals(6, resultado.getStockDisponible());
        assertTrue(resultado.getDisponible());
    }

    @Test
    @DisplayName("consultar disponibilidad de producto inactivo retorna 404")
    void consultarDisponibilidadProductoInactivo() {
        when(productoRepository.findByCodigoAndActivoTrue("GA001")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inventarioService.consultarDisponibilidad("GA001"));
    }

    @Test
    @DisplayName("obtener inventario por codigo de producto")
    void obtenerInventarioPorCodigo() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 8, 2);

        when(productoRepository.findByCodigo("GA001")).thenReturn(Optional.of(producto));
        when(inventarioRepository.findByProductoId(1L)).thenReturn(Optional.of(inventario));

        InventarioResponseDTO resultado = inventarioService.obtenerPorCodigo("GA001");

        assertEquals("GA001", resultado.getCodigoProducto());
        assertEquals(8, resultado.getStockFisico());
        assertEquals(2, resultado.getStockReservado());
        assertEquals(6, resultado.getStockDisponible());
    }

    @Test
    @DisplayName("actualizar stock fisico")
    void actualizarStockFisico() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 8, 2);

        ActualizarStockRequestDTO dto = new ActualizarStockRequestDTO();
        dto.setStockFisico(20);
        dto.setStockCritico(4);

        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));
        when(inventarioRepository.saveAndFlush(any(Inventario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        InventarioResponseDTO resultado = inventarioService.actualizarStock("ga001", dto);

        assertEquals(20, resultado.getStockFisico());
        assertEquals(18, resultado.getStockDisponible());
        assertEquals(4, resultado.getStockCritico());
    }

    @Test
    @DisplayName("no permitir stock fisico menor al reservado")
    void noPermitirStockBajoReservado() {
        Producto producto = producto("GA001");
        Inventario inventario = inventario(producto, 8, 5);

        ActualizarStockRequestDTO dto = new ActualizarStockRequestDTO();
        dto.setStockFisico(4);

        when(inventarioRepository.findByProductoCodigoForUpdate("GA001")).thenReturn(Optional.of(inventario));

        assertThrows(ConflictException.class, () -> inventarioService.actualizarStock("GA001", dto));
        verify(inventarioRepository, never()).saveAndFlush(any(Inventario.class));
    }

    @Test
    @DisplayName("listar inventario")
    void listarInventario() {
        Producto producto = producto("GA001");
        when(inventarioRepository.findAllConProducto()).thenReturn(List.of(inventario(producto, 8, 2)));

        List<InventarioResponseDTO> resultado = inventarioService.listar();

        assertEquals(1, resultado.size());
        assertEquals(6, resultado.get(0).getStockDisponible());
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

    private Inventario inventario(int fisico, int reservado) {
        return inventario(producto("GA001"), fisico, reservado);
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
}
