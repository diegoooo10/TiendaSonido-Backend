package producto_service.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import producto_service.model.Categoria;
import producto_service.model.Inventario;
import producto_service.model.Producto;
import producto_service.repository.CategoriaRepository;
import producto_service.repository.InventarioRepository;
import producto_service.repository.ProductoRepository;
import producto_service.repository.ReservaStockRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:seguridad_producto;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private ReservaStockRepository reservaStockRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        reservaStockRepository.deleteAll();
        inventarioRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();

        categoria = new Categoria();
        categoria.setNombre("Guitarras Acusticas");
        categoria.setDescripcion("Categoria de prueba");
        categoria.setActivo(true);
        categoria = categoriaRepository.saveAndFlush(categoria);

        Producto producto = new Producto();
        producto.setCodigo("GA001");
        producto.setNombre("Guitarra Acustica Folk");
        producto.setDescripcion("Descripcion");
        producto.setMarca("Yamaha");
        producto.setModelo("F310");
        producto.setImagen("img/guitarra.jpg");
        producto.setPrecio(new BigDecimal("129990.00"));
        producto.setEnOferta(false);
        producto.setActivo(true);
        producto.setCategoria(categoria);
        producto = productoRepository.saveAndFlush(producto);

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setStockFisico(5);
        inventario.setStockReservado(0);
        inventario.setStockCritico(3);
        inventarioRepository.saveAndFlush(inventario);
    }

    @Test
    @DisplayName("el catalogo es publico sin token")
    void catalogoPublicoSinToken() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("GA001"))
                .andExpect(jsonPath("$[0].stockDisponible").value(5));

        mockMvc.perform(get("/api/productos/GA001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioEfectivo").value(129990.00));

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Guitarras Acusticas"));

        mockMvc.perform(get("/api/inventario/disponibilidad/GA001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockDisponible").value(5))
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    @DisplayName("crear producto sin token retorna 401")
    void crearProductoSinToken() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA099", "Producto sin token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    @DisplayName("CLIENTE no puede crear productos")
    void clienteNoPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA098", "Producto cliente")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    @DisplayName("VENDEDOR puede crear productos")
    void vendedorPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA097", "Producto vendedor")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("GA097"))
                .andExpect(jsonPath("$.stockDisponible").value(0));
    }

    @Test
    @DisplayName("ADMIN y EMPLEADO legacy siguen funcionando durante la transicion")
    void rolesLegacyCompatibles() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA096", "Producto admin legacy")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA095", "Producto empleado legacy")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("solo ADMINISTRADOR puede crear categorias")
    void permisosDeCategorias() throws Exception {
        String body = """
                {"nombre":"Nueva categoria","descripcion":"Prueba","activo":true}
                """;

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", bearer("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", bearer("ADMINISTRADOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Nueva categoria"));
    }

    @Test
    @DisplayName("inventario general solo para gestion")
    void inventarioGeneralSoloGestion() throws Exception {
        mockMvc.perform(get("/api/inventario")
                        .header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/inventario")
                        .header("Authorization", bearer("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoProducto").value("GA001"));
    }

    @Test
    @DisplayName("VENDEDOR reserva y confirma stock")
    void reservarYConfirmarStock() throws Exception {
        String reserva = """
                {"pedidoId":"PED-1","codigoProducto":"GA001","cantidad":2,"referencia":"REF-1"}
                """;

        mockMvc.perform(post("/api/inventario/reservas")
                        .header("Authorization", bearer("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserva))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.stockDisponible").value(3));

        mockMvc.perform(post("/api/inventario/reservas/REF-1/confirmar")
                        .header("Authorization", bearer("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.stockDisponible").value(3));
    }

    @Test
    @DisplayName("CLIENTE no puede reservar stock")
    void clienteNoPuedeReservarStock() throws Exception {
        mockMvc.perform(post("/api/inventario/reservas")
                        .header("Authorization", bearer("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":"PED-1","codigoProducto":"GA001","cantidad":1,"referencia":"REF-1"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("reserva superior al stock disponible retorna 409")
    void reservaSinStockSuficiente() throws Exception {
        mockMvc.perform(post("/api/inventario/reservas")
                        .header("Authorization", bearer("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":"PED-1","codigoProducto":"GA001","cantidad":99,"referencia":"REF-99"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("reintento de reserva con la misma referencia es idempotente")
    void reservaIdempotente() throws Exception {
        String reserva = """
                {"pedidoId":"PED-1","codigoProducto":"GA001","cantidad":2,"referencia":"REF-1"}
                """;

        mockMvc.perform(post("/api/inventario/reservas")
                        .header("Authorization", bearer("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserva))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventario/reservas")
                        .header("Authorization", bearer("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.stockDisponible").value(3));
    }

    @Test
    @DisplayName("token invalido retorna 401 en endpoint protegido")
    void tokenInvalido() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer token-invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA094", "Producto token invalido")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS permite el frontend local")
    void corsPermiteFrontendLocal() throws Exception {
        mockMvc.perform(options("/api/productos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String bearer(String rol) {
        String token = Jwts.builder()
                .setSubject("usuario-prueba")
                .claim("rol", rol)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
        return "Bearer " + token;
    }

    private String productoJson(String codigo, String nombre) {
        return """
                {
                  "codigo":"%s",
                  "nombre":"%s",
                  "descripcion":"Producto de prueba",
                  "marca":"Yamaha",
                  "modelo":"F310",
                  "imagen":"img/guitarra.jpg",
                  "precio":1000.00,
                  "enOferta":false,
                  "activo":true,
                  "categoriaId":%d
                }
                """.formatted(codigo, nombre, categoria.getId());
    }
}
