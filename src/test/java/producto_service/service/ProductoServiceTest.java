package producto_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import producto_service.config.InventarioProperties;
import producto_service.dto.ProductoRequestDTO;
import producto_service.dto.ProductoResponseDTO;
import producto_service.exception.BusinessException;
import producto_service.exception.ConflictException;
import producto_service.exception.ResourceNotFoundException;
import producto_service.model.Categoria;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.repository.CategoriaRepository;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private InventarioRepository inventarioRepository;

    private ProductoService productoService;

    @BeforeEach
    void setUp() {
        productoService = new ProductoService(
                productoRepository,
                categoriaRepository,
                inventarioRepository,
                new InventarioProperties()
        );
    }

    @Test
    @DisplayName("crear producto normaliza codigo, usa BigDecimal y crea inventario en cero")
    void crearProductoCorrectamente() {
        Categoria categoria = categoria();
        ProductoRequestDTO dto = request("ga001", "Guitarra Acustica Folk", "129990");

        when(productoRepository.existsByCodigo("GA001")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> {
            Producto producto = invocacion.getArgument(0);
            producto.setId(1L);
            return producto;
        });
        when(inventarioRepository.save(any(Inventario.class))).thenAnswer(invocacion -> {
            Inventario inventario = invocacion.getArgument(0);
            inventario.setId(1L);
            return inventario;
        });

        ProductoResponseDTO resultado = productoService.guardar(dto);

        assertNotNull(resultado);
        assertEquals("GA001", resultado.getCodigo());
        assertEquals(0, resultado.getStockDisponible());
        assertEquals(0, resultado.getStock());
        assertEquals(0, new BigDecimal("129990.00").compareTo(resultado.getPrecio()));
        assertEquals(0, new BigDecimal("129990.00").compareTo(resultado.getPrecioEfectivo()));
        assertEquals(1L, resultado.getCategoriaId());
        assertTrue(resultado.getActivo());
    }

    @Test
    @DisplayName("crear producto rechaza codigo duplicado")
    void crearProductoConCodigoDuplicado() {
        ProductoRequestDTO dto = request("GA001", "Guitarra", "1000");
        when(productoRepository.existsByCodigo("GA001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> productoService.guardar(dto));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    @DisplayName("crear producto rechaza oferta sin precioOferta")
    void crearProductoEnOfertaSinPrecioOferta() {
        ProductoRequestDTO dto = request("GA001", "Guitarra", "1000");
        dto.setEnOferta(true);

        when(productoRepository.existsByCodigo("GA001")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));

        assertThrows(BusinessException.class, () -> productoService.guardar(dto));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    @DisplayName("crear producto rechaza precioOferta mayor o igual al precio normal")
    void crearProductoConOfertaInvalida() {
        ProductoRequestDTO dto = request("GA001", "Guitarra", "1000");
        dto.setEnOferta(true);
        dto.setPrecioOferta(new BigDecimal("1000"));

        when(productoRepository.existsByCodigo("GA001")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));

        assertThrows(BusinessException.class, () -> productoService.guardar(dto));
    }

    @Test
    @DisplayName("crear producto rechaza precio negativo")
    void crearProductoConPrecioNegativo() {
        ProductoRequestDTO dto = request("GA001", "Guitarra", "-1");

        when(productoRepository.existsByCodigo("GA001")).thenReturn(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria()));

        assertThrows(BusinessException.class, () -> productoService.guardar(dto));
    }

    @Test
    @DisplayName("precio efectivo usa la oferta solo cuando enOferta es true")
    void precioEfectivoRespetaBanderaDeOferta() {
        Categoria categoria = categoria();

        Producto conOferta = producto(categoria, "GA001", new BigDecimal("1000.00"));
        conOferta.setEnOferta(true);
        conOferta.setPrecioOferta(new BigDecimal("800.00"));

        Producto sinOferta = producto(categoria, "GA002", new BigDecimal("1000.00"));
        sinOferta.setEnOferta(false);
        sinOferta.setPrecioOferta(new BigDecimal("800.00"));

        assertEquals(0, new BigDecimal("800.00").compareTo(ProductoMapperParaTest.precioEfectivo(conOferta)));
        assertEquals(0, new BigDecimal("1000.00").compareTo(ProductoMapperParaTest.precioEfectivo(sinOferta)));
    }

    @Test
    @DisplayName("listar productos delega filtros al repositorio")
    void listarProductosConFiltros() {
        Producto producto = producto(categoria(), "GA001", new BigDecimal("1000.00"));
        when(productoRepository.buscar("guitarra", 1L, "Yamaha", true)).thenReturn(List.of(producto));

        List<ProductoResponseDTO> resultado = productoService.listar("  guitarra ", 1L, "Yamaha", true);

        assertEquals(1, resultado.size());
        assertEquals("GA001", resultado.get(0).getCodigo());
        assertEquals(6, resultado.get(0).getStockDisponible());
    }

    @Test
    @DisplayName("obtener por codigo retorna producto activo")
    void obtenerPorCodigoExistente() {
        Producto producto = producto(categoria(), "GA001", new BigDecimal("1000.00"));
        when(productoRepository.findByCodigoAndActivoTrue("GA001")).thenReturn(Optional.of(producto));

        ProductoResponseDTO resultado = productoService.obtenerPorCodigo("ga001");

        assertEquals("GA001", resultado.getCodigo());
        assertEquals(6, resultado.getStockDisponible());
    }

    @Test
    @DisplayName("obtener por codigo inexistente retorna 404 de negocio")
    void obtenerPorCodigoInexistente() {
        when(productoRepository.findByCodigoAndActivoTrue("XX999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productoService.obtenerPorCodigo("XX999"));
    }

    @Test
    @DisplayName("actualizar producto conserva el codigo de la ruta")
    void actualizarProductoCorrectamente() {
        Categoria categoria = categoria();
        Producto existente = producto(categoria, "GA001", new BigDecimal("1000.00"));
        ProductoRequestDTO dto = request("GA001", "Guitarra Actualizada", "1200");
        dto.setDescripcion("Nueva descripcion");

        when(productoRepository.findByCodigo("GA001")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ProductoResponseDTO resultado = productoService.actualizar("ga001", dto);

        assertEquals("Guitarra Actualizada", resultado.getNombre());
        assertEquals(0, new BigDecimal("1200.00").compareTo(resultado.getPrecio()));
        assertEquals("Nueva descripcion", resultado.getDescripcion());
    }

    @Test
    @DisplayName("actualizar rechaza codigo del cuerpo distinto al de la ruta")
    void actualizarConCodigoDistinto() {
        Producto existente = producto(categoria(), "GA001", new BigDecimal("1000.00"));
        ProductoRequestDTO dto = request("GA002", "Guitarra", "1000");

        when(productoRepository.findByCodigo("GA001")).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> productoService.actualizar("GA001", dto));
    }

    @Test
    @DisplayName("desactivar hace baja logica sin borrar el producto")
    void desactivarProducto() {
        Producto existente = producto(categoria(), "GA001", new BigDecimal("1000.00"));
        when(productoRepository.findByCodigo("GA001")).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        productoService.desactivar("GA001");

        assertFalse(existente.getActivo());
        verify(productoRepository).save(existente);
        verify(productoRepository, never()).delete(any(Producto.class));
        verify(productoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("buscar por categoria inexistente retorna 404")
    void buscarPorCategoriaInexistente() {
        when(categoriaRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> productoService.buscarPorCategoria(99L));
    }

    @Test
    @DisplayName("listar ofertas consulta solo productos activos en oferta")
    void listarOfertas() {
        Producto producto = producto(categoria(), "GA001", new BigDecimal("1000.00"));
        producto.setEnOferta(true);
        producto.setPrecioOferta(new BigDecimal("800.00"));
        when(productoRepository.findByActivoTrueAndEnOfertaTrueOrderByNombreAsc()).thenReturn(List.of(producto));

        List<ProductoResponseDTO> resultado = productoService.listarOfertas();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getEnOferta());
        assertEquals(0, new BigDecimal("800.00").compareTo(resultado.get(0).getPrecioEfectivo()));
    }

    private ProductoRequestDTO request(String codigo, String nombre, String precio) {
        ProductoRequestDTO dto = new ProductoRequestDTO();
        dto.setCodigo(codigo);
        dto.setNombre(nombre);
        dto.setDescripcion("Descripcion");
        dto.setMarca("Yamaha");
        dto.setModelo("F310");
        dto.setImagen("img/guitarra.jpg");
        dto.setPrecio(new BigDecimal(precio));
        dto.setEnOferta(false);
        dto.setActivo(true);
        dto.setCategoriaId(1L);
        return dto;
    }

    private Categoria categoria() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Guitarras Acusticas");
        categoria.setDescripcion("Categoria");
        categoria.setActivo(true);
        return categoria;
    }

    private Producto producto(Categoria categoria, String codigo, BigDecimal precio) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setCodigo(codigo);
        producto.setNombre("Guitarra");
        producto.setDescripcion("Descripcion");
        producto.setMarca("Yamaha");
        producto.setModelo("F310");
        producto.setImagen("img/guitarra.jpg");
        producto.setPrecio(precio);
        producto.setEnOferta(false);
        producto.setActivo(true);
        producto.setCategoria(categoria);

        Inventario inventario = new Inventario();
        inventario.setId(1L);
        inventario.setProducto(producto);
        inventario.setStockFisico(8);
        inventario.setStockReservado(2);
        inventario.setStockCritico(3);
        producto.setInventario(inventario);

        return producto;
    }

    /**
     * Pequeno adaptador para probar la regla de precio sin exponer el servicio.
     */
    private static final class ProductoMapperParaTest {
        private static BigDecimal precioEfectivo(Producto producto) {
            return producto_service.mapper.ProductoMapper.calcularPrecioEfectivo(producto);
        }
    }
}
