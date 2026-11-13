package producto_service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

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

    private static final String OTRO_SECRETO = "otro-secreto-de-prueba-de-32-bytes-o-mas-0001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

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

    // ---------------------------------------------------------------------
    // Contrato del token
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("un token Nimbus HS256 expone el subject (correo) y el claim rol")
    void tokenValidoExponeSubjectYRol() {
        Jwt jwt = jwtDecoder.decode(token("ADMIN"));

        assertEquals("correo@tienda.test", jwt.getSubject());
        assertEquals("ADMIN", jwt.getClaimAsString("rol"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
    }

    @Test
    @DisplayName("un token expirado retorna 401 en endpoint protegido")
    void tokenExpiradoRechazado() throws Exception {
        String expirado = firmar(
                JwtClaimsSet.builder()
                        .subject("correo@tienda.test")
                        .claim("rol", "ADMIN")
                        .issuedAt(Instant.now().minusSeconds(7200))
                        .expiresAt(Instant.now().minusSeconds(3600))
                        .build(),
                jwtSecret
        );

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + expirado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA090", "Producto expirado")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(401));
    }

    @Test
    @DisplayName("un token firmado con otro secreto retorna 401")
    void firmaIncorrectaRechazada() throws Exception {
        String ajeno = firmar(claims("ADMIN"), OTRO_SECRETO);

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + ajeno)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA091", "Producto firma ajena")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un token sin claim rol autentica pero no autoriza")
    void tokenSinRolRechazado() throws Exception {
        String sinRol = firmar(
                JwtClaimsSet.builder()
                        .subject("correo@tienda.test")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build(),
                jwtSecret
        );

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + sinRol)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA092", "Producto sin rol")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(403));
    }

    @Test
    @DisplayName("un token malformado retorna 401")
    void tokenMalformado() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer no-es-un-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA094", "Producto token invalido")))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------------
    // Catalogo publico y matriz de roles
    // ---------------------------------------------------------------------

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
    @DisplayName("CLIENTE solo puede leer el catalogo")
    void clienteSoloLecturaCatalogo() throws Exception {
        mockMvc.perform(get("/api/productos").header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/categorias").header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/ofertas").header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/inventario/disponibilidad/GA001")
                        .header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/inventario/GA001/stock")
                        .header("Authorization", bearer("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockFisico":10}
                                """))
                .andExpect(status().isForbidden());
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
    @DisplayName("ADMIN puede crear productos")
    void adminPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA097", "Producto admin")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("GA097"))
                .andExpect(jsonPath("$.stockDisponible").value(0));
    }

    @Test
    @DisplayName("EMPLEADO no puede crear ni actualizar productos")
    void empleadoNoGestionaProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA096", "Producto empleado")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/productos/GA001")
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA001", "Producto actualizado")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/productos/GA001")
                        .header("Authorization", bearer("EMPLEADO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("solo ADMIN puede crear categorias")
    void permisosDeCategorias() throws Exception {
        String body = """
                {"nombre":"Nueva categoria","descripcion":"Prueba","activo":true}
                """;

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Nueva categoria"));
    }

    @Test
    @DisplayName("inventario general solo para ADMIN y EMPLEADO")
    void inventarioGeneralSoloGestion() throws Exception {
        mockMvc.perform(get("/api/inventario")
                        .header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/inventario")
                        .header("Authorization", bearer("EMPLEADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoProducto").value("GA001"));

        mockMvc.perform(get("/api/inventario")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EMPLEADO actualiza stock y gestiona reservas")
    void empleadoGestionaInventarioYReservas() throws Exception {
        mockMvc.perform(put("/api/inventario/GA001/stock")
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockFisico":8,"stockCritico":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockFisico").value(8));

        mockMvc.perform(post("/api/inventario/reservas")
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":"PED-EMP","codigoProducto":"GA001","cantidad":2,"referencia":"REF-EMP"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RESERVADA"));

        mockMvc.perform(post("/api/inventario/reservas/REF-EMP/confirmar")
                        .header("Authorization", bearer("EMPLEADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        mockMvc.perform(post("/api/inventario/reservas")
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pedidoId":"PED-EMP-2","codigoProducto":"GA001","cantidad":1,"referencia":"REF-EMP-2"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventario/reservas/REF-EMP-2/liberar")
                        .header("Authorization", bearer("EMPLEADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LIBERADA"));
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
                        .header("Authorization", bearer("EMPLEADO"))
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
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserva))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/inventario/reservas")
                        .header("Authorization", bearer("EMPLEADO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reserva))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.stockDisponible").value(3));
    }

    // ---------------------------------------------------------------------
    // Endpoints administrativos
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("ADMIN consulta productos activos e inactivos sin exponerlos en el catalogo")
    void adminListaProductosActivosEInactivos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA002", "Producto inactivo", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("GA001"));

        mockMvc.perform(get("/api/productos/GA002"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/productos/admin")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].codigo").value("GA002"))
                .andExpect(jsonPath("$[1].activo").value(false));

        mockMvc.perform(get("/api/productos/admin/GA002")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("GA002"))
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/productos/admin/paginado")
                        .header("Authorization", bearer("ADMIN"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("EMPLEADO y CLIENTE no acceden a los endpoints administrativos de productos")
    void empleadoYClienteNoAccedenAdminProductos() throws Exception {
        mockMvc.perform(get("/api/productos/admin")
                        .header("Authorization", bearer("EMPLEADO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/productos/admin")
                        .header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/productos/admin/GA001")
                        .header("Authorization", bearer("EMPLEADO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/productos/admin/paginado")
                        .header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/productos/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN consulta categorias inactivas sin exponerlas en el listado publico")
    void adminListaCategoriasInactivas() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Categoria inactiva","descripcion":"Prueba","activo":false}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Guitarras Acusticas"));

        mockMvc.perform(get("/api/categorias/admin")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Categoria inactiva"))
                .andExpect(jsonPath("$[0].activo").value(false));

        mockMvc.perform(get("/api/categorias/admin")
                        .header("Authorization", bearer("EMPLEADO")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/categorias/admin")
                        .header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN actualiza, desactiva y reactiva un producto")
    void adminActualizaDesactivaYReactivaProducto() throws Exception {
        mockMvc.perform(put("/api/productos/GA001")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA001", "Guitarra actualizada")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Guitarra actualizada"));

        mockMvc.perform(delete("/api/productos/GA001")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/productos/GA001"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/productos/admin/GA001")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(put("/api/productos/GA001")
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productoJson("GA001", "Guitarra reactivada")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get("/api/productos/GA001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("no se puede desactivar una categoria con productos activos")
    void noDesactivarCategoriaConProductosActivos() throws Exception {
        String body = """
                {"nombre":"Guitarras Acusticas","descripcion":"Categoria de prueba","activo":false}
                """;

        mockMvc.perform(put("/api/categorias/" + categoria.getId())
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value(
                        "No se puede desactivar una categoria con productos activos asociados"
                ));

        mockMvc.perform(delete("/api/productos/GA001")
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/categorias/" + categoria.getId())
                        .header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("CORS permite el frontend local y metodos de gestion")
    void corsPermiteFrontendLocal() throws Exception {
        mockMvc.perform(options("/api/productos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/inventario/GA001/stock")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Authorization,Cache-Control,Pragma"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    // ---------------------------------------------------------------------
    // Helpers de token y cuerpos JSON
    // ---------------------------------------------------------------------

    private JwtClaimsSet claims(String rol) {
        return JwtClaimsSet.builder()
                .subject("correo@tienda.test")
                .claim("rol", rol)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private String token(String rol) {
        return firmar(claims(rol), jwtSecret);
    }

    private String bearer(String rol) {
        return "Bearer " + token(rol);
    }

    private String firmar(JwtClaimsSet claims, String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String productoJson(String codigo, String nombre) {
        return productoJson(codigo, nombre, true);
    }

    private String productoJson(String codigo, String nombre, boolean activo) {
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
                  "activo":%s,
                  "categoriaId":%d
                }
                """.formatted(codigo, nombre, activo, categoria.getId());
    }
}
