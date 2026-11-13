package producto_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import producto_service.dto.ReservaStockRequestDTO;
import producto_service.model.Categoria;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.repository.CategoriaRepository;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;
import producto_service.repository.ReservaStockRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:concurrencia_producto;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class ReservaConcurrenteIntegrationTest {

    @Autowired
    private ReservaStockService reservaStockService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private ReservaStockRepository reservaStockRepository;

    @BeforeEach
    void setUp() {
        reservaStockRepository.deleteAll();
        inventarioRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNombre("Guitarras Acusticas");
        categoria.setActivo(true);
        categoria = categoriaRepository.saveAndFlush(categoria);

        crearProductoConStock(categoria, "GA001", "Guitarra", 1);
        crearProductoConStock(categoria, "GA002", "Bajo", 5);
    }

    @Test
    @DisplayName("dos reservas simultaneas no pueden sobrevender la ultima unidad")
    void dosReservasSimultaneasNoSobrevenden() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        try {
            Future<Boolean> primera = executor.submit(
                    () -> intentarReserva("GA001", "PED-1", "REF-1", listos, inicio)
            );
            Future<Boolean> segunda = executor.submit(
                    () -> intentarReserva("GA001", "PED-1", "REF-2", listos, inicio)
            );

            assertTrue(listos.await(5, TimeUnit.SECONDS));
            inicio.countDown();

            int exitosas = (primera.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (segunda.get(10, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, exitosas);
            assertEquals(1, reservaStockRepository.count());

            Inventario inventario = inventarioRepository.findByProductoCodigo("GA001").orElseThrow();
            assertEquals(1, inventario.getStockFisico());
            assertEquals(1, inventario.getStockReservado());
            assertEquals(0, inventario.getStockDisponible());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("dos reservas simultaneas con la misma referencia son idempotentes")
    void dosReservasSimultaneasMismaReferenciaSonIdempotentes() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        try {
            Future<Boolean> primera = executor.submit(
                    () -> intentarReserva("GA002", "PED-2", "REF-UNICA", listos, inicio)
            );
            Future<Boolean> segunda = executor.submit(
                    () -> intentarReserva("GA002", "PED-2", "REF-UNICA", listos, inicio)
            );

            assertTrue(listos.await(5, TimeUnit.SECONDS));
            inicio.countDown();

            assertTrue(primera.get(10, TimeUnit.SECONDS));
            assertTrue(segunda.get(10, TimeUnit.SECONDS));

            assertEquals(1, reservaStockRepository.count());
            assertEquals(1, reservaStockRepository.findByReferencia("REF-UNICA").orElseThrow().getCantidad());

            Inventario inventario = inventarioRepository.findByProductoCodigo("GA002").orElseThrow();
            assertEquals(5, inventario.getStockFisico());
            assertEquals(1, inventario.getStockReservado());
            assertEquals(4, inventario.getStockDisponible());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("la misma referencia concurrente con otro producto solo crea una reserva")
    void mismaReferenciaConOtroProductoNoDuplica() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        try {
            Future<Boolean> primera = executor.submit(
                    () -> intentarReserva("GA001", "PED-3", "REF-CRUZADA", listos, inicio)
            );
            Future<Boolean> segunda = executor.submit(
                    () -> intentarReserva("GA002", "PED-3", "REF-CRUZADA", listos, inicio)
            );

            assertTrue(listos.await(5, TimeUnit.SECONDS));
            inicio.countDown();

            int exitosas = (primera.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (segunda.get(10, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, exitosas);
            assertEquals(1, reservaStockRepository.count());

            Inventario ga001 = inventarioRepository.findByProductoCodigo("GA001").orElseThrow();
            Inventario ga002 = inventarioRepository.findByProductoCodigo("GA002").orElseThrow();
            assertEquals(1, ga001.getStockReservado() + ga002.getStockReservado());
        } finally {
            executor.shutdownNow();
        }
    }

    private void crearProductoConStock(Categoria categoria, String codigo, String nombre, int stock) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal("1000.00"));
        producto.setEnOferta(false);
        producto.setActivo(true);
        producto.setCategoria(categoria);
        producto = productoRepository.saveAndFlush(producto);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setStockFisico(stock);
        inventario.setStockReservado(0);
        inventario.setStockCritico(1);
        inventarioRepository.saveAndFlush(inventario);
    }

    private boolean intentarReserva(
            String codigo,
            String pedidoId,
            String referencia,
            CountDownLatch listos,
            CountDownLatch inicio
    ) {
        listos.countDown();
        try {
            inicio.await(5, TimeUnit.SECONDS);

            ReservaStockRequestDTO dto = new ReservaStockRequestDTO();
            dto.setPedidoId(pedidoId);
            dto.setCodigoProducto(codigo);
            dto.setCantidad(1);
            dto.setReferencia(referencia);

            reservaStockService.reservar(dto);
            return true;
        } catch (RuntimeException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
