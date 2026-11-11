package producto_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import producto_service.model.Inventario;
import producto_service.repository.CategoriaRepository;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo_inicial;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "catalogo.carga-inicial.enabled=true"
})
@ActiveProfiles("test")
class CatalogoInitializerIntegrationTest {

    @Autowired
    private CatalogoInitializer catalogoInitializer;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Test
    @DisplayName("la carga inicial importa todo el catalogo y es idempotente")
    void cargaInicialEsIdempotente() throws Exception {
        assertEquals(51, productoRepository.count());
        assertEquals(10, categoriaRepository.count());
        assertEquals(51, inventarioRepository.count());
        assertEquals(
                365,
                inventarioRepository.findAll().stream().mapToInt(Inventario::getStockFisico).sum()
        );

        catalogoInitializer.run();

        assertEquals(51, productoRepository.count());
        assertEquals(10, categoriaRepository.count());
        assertEquals(51, inventarioRepository.count());
    }
}
