package producto_service.config;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import producto_service.model.Categoria;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.repository.CategoriaRepository;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;

/**
 * Carga idempotente del catalogo oficial de Tienda Sonido Vivo.
 *
 * Los productos se identifican por su codigo comercial. Si un producto ya
 * existe, no se sobrescriben sus datos ni su stock. El stock inicial se toma
 * exclusivamente del catalogo; no se generan cantidades artificiales.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogoInitializer implements CommandLineRunner {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final InventarioRepository inventarioRepository;
    private final InventarioProperties inventarioProperties;

    @Value("${catalogo.carga-inicial.enabled:true}")
    private boolean habilitada;

    @Value("${catalogo.carga-inicial.archivo:classpath:catalogo/productos.json}")
    private String ubicacionCatalogo;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!habilitada) {
            log.info("Carga inicial de catalogo deshabilitada");
            return;
        }

        List<CatalogoProducto> catalogo = leerCatalogo();
        validarCatalogo(catalogo);

        int categoriasCreadas = 0;
        int productosCreados = 0;
        int productosExistentes = 0;
        int inventariosCreados = 0;

        for (CatalogoProducto item : catalogo) {
            Categoria categoria = categoriaRepository.findByNombreIgnoreCase(item.categoria().trim())
                    .orElse(null);

            if (categoria == null) {
                categoria = new Categoria();
                categoria.setNombre(item.categoria().trim());
                categoria.setActivo(true);
                categoria = categoriaRepository.save(categoria);
                categoriasCreadas++;
            }

            String codigo = item.codigo().trim().toUpperCase(Locale.ROOT);
            Producto producto = productoRepository.findByCodigo(codigo).orElse(null);

            if (producto == null) {
                producto = new Producto();
                producto.setCodigo(codigo);
                producto.setNombre(item.nombre().trim());
                producto.setDescripcion(item.descripcion());
                producto.setMarca(item.marca());
                producto.setModelo(item.modelo());
                producto.setImagen(item.imagen());
                producto.setPrecio(item.precio());
                producto.setPrecioOferta(null);
                producto.setEnOferta(false);
                producto.setActivo(true);
                producto.setCategoria(categoria);
                producto = productoRepository.save(producto);
                productosCreados++;
            } else {
                productosExistentes++;
            }

            if (!inventarioRepository.existsByProductoId(producto.getId())) {
                Inventario inventario = new Inventario();
                inventario.setProducto(producto);
                inventario.setStockFisico(item.stock());
                inventario.setStockReservado(0);
                inventario.setStockCritico(inventarioProperties.getStockCriticoInicial());
                inventarioRepository.save(inventario);
                inventariosCreados++;
            }
        }

        log.info(
                "Catalogo procesado: {} productos en archivo, {} creados, {} ya existentes, "
                        + "{} categorias nuevas, {} inventarios nuevos",
                catalogo.size(),
                productosCreados,
                productosExistentes,
                categoriasCreadas,
                inventariosCreados
        );
    }

    private List<CatalogoProducto> leerCatalogo() throws Exception {
        Resource recurso = resourceLoader.getResource(ubicacionCatalogo);

        if (!recurso.exists()) {
            throw new IllegalStateException("No se encontro el catalogo en " + ubicacionCatalogo);
        }

        try (InputStream inputStream = recurso.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<CatalogoProducto>>() {
            });
        }
    }

    private void validarCatalogo(List<CatalogoProducto> catalogo) {
        if (catalogo == null || catalogo.isEmpty()) {
            throw new IllegalStateException("El catalogo no contiene productos");
        }

        Set<String> codigos = new HashSet<>();

        for (CatalogoProducto item : catalogo) {
            if (item.codigo() == null || item.codigo().isBlank()) {
                throw new IllegalStateException("El catalogo contiene un producto sin codigo");
            }
            if (item.categoria() == null || item.categoria().isBlank()) {
                throw new IllegalStateException("El producto " + item.codigo() + " no tiene categoria");
            }
            if (item.nombre() == null || item.nombre().isBlank()) {
                throw new IllegalStateException("El producto " + item.codigo() + " no tiene nombre");
            }
            if (item.stock() == null || item.stock() < 0) {
                throw new IllegalStateException("El producto " + item.codigo() + " tiene un stock invalido");
            }
            if (item.precio() == null || item.precio().signum() <= 0) {
                throw new IllegalStateException("El producto " + item.codigo() + " tiene un precio invalido");
            }

            String codigo = item.codigo().trim().toUpperCase(Locale.ROOT);
            if (!codigos.add(codigo)) {
                throw new IllegalStateException("El catalogo contiene el codigo duplicado " + codigo);
            }
        }
    }
}
