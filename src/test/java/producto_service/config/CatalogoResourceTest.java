package producto_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class CatalogoResourceTest {

    @Test
    @DisplayName("el catalogo oficial contiene los 51 productos y 10 categorias")
    void catalogoOficialCompleto() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<CatalogoProducto> catalogo;

        try (InputStream inputStream = new ClassPathResource("catalogo/productos.json").getInputStream()) {
            catalogo = objectMapper.readValue(inputStream, new TypeReference<List<CatalogoProducto>>() {
            });
        }

        assertNotNull(catalogo);
        assertEquals(51, catalogo.size());

        Set<String> codigos = new HashSet<>();
        Set<String> categorias = new HashSet<>();
        int stockTotal = 0;

        for (CatalogoProducto producto : catalogo) {
            assertNotNull(producto.codigo());
            assertNotNull(producto.nombre());
            assertNotNull(producto.descripcion());
            assertNotNull(producto.imagen());
            assertNotNull(producto.precio());
            assertTrue(producto.precio().signum() > 0);
            assertTrue(producto.stock() >= 0);

            assertTrue(codigos.add(producto.codigo()), "Codigo duplicado: " + producto.codigo());
            categorias.add(producto.categoria());
            stockTotal += producto.stock();
        }

        assertEquals(51, codigos.size());
        assertEquals(10, categorias.size());
        assertEquals(365, stockTotal);
    }
}
