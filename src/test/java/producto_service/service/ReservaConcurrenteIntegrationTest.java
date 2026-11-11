package producto_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
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

        Producto producto = new Producto();
        producto.setCodigo("GA001");
        producto.setNombre("Guitarra");
        producto.setPrecio(new BigDecimal("1000.00"));
        producto.setEnOferta(false);
        producto.setActivo(true);
        producto.setCategoria(categoria);
        producto = productoRepository.saveAndFlush(producto);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setStockFisico(1);
        inventario.setStockReservado(0);
        inventario.setStockCritico(1);
        inventarioRepository.saveAndFlush(inventario);
    }

    @Test
    @DisplayName("dos reservas simultaneas no pueden sobrevender la ultima unidad")
    void dosReservasSimultaneasNoSobrevenden() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);

        try {
            Future<Boolean> primera = executor.submit(() -> intentarReserva("REF-1", listos, inicio));
            Future<Boolean> segunda = executor.submit(() -> intentarReserva("REF-2", listos, inicio));

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

    private boolean intentarReserva(String referencia, CountDownLatch listos, CountDownLatch inicio) {
        listos.countDown();
        try {
            inicio.await(5, TimeUnit.SECONDS);

            ReservaStockRequestDTO dto = new ReservaStockRequestDTO();
            dto.setPedidoId("PED-1");
            dto.setCodigoProducto("GA001");
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
