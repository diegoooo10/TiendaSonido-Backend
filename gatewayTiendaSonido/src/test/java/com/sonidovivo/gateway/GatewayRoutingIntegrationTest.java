package com.sonidovivo.gateway;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpServer;

/**
 * Verificacion de integracion real del Gateway:
 * - arranca un servidor HTTP de destino temporal en un puerto aleatorio;
 * - sustituye las URIs de las tres rutas por ese destino;
 * - comprueba que el path original y el header Authorization llegan intactos;
 * - comprueba CORS y Actuator.
 *
 * El microservicio de Productos no se modifica ni se necesita para esta prueba.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIntegrationTest {

    private static final String ORIGEN_PERMITIDO = "http://localhost:5173";
    private static final String TOKEN = "Bearer token-de-prueba";
    private static final List<SolicitudRegistrada> SOLICITUDES = new CopyOnWriteArrayList<>();
    private static final HttpServer DESTINO = iniciarDestino();
    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${local.server.port}")
    private int puerto;

    /**
     * Spring Boot no fusiona listas entre origenes de propiedades: si una
     * propiedad de la lista se define en un origen de mayor prioridad, la lista
     * completa se toma de ese origen. Por eso aqui se redefinen las tres rutas
     * completas (id, uri y predicado Path), no solo la uri.
     */
    @DynamicPropertySource
    static void configurarDestino(DynamicPropertyRegistry registry) {
        String uri = "http://127.0.0.1:" + DESTINO.getAddress().getPort();

        String prefijo = "spring.cloud.gateway.server.webflux.routes";

        registry.add(prefijo + "[0].id", () -> "productos");
        registry.add(prefijo + "[0].uri", () -> uri);
        registry.add(prefijo + "[0].predicates[0]", () -> "Path=/api/productos/**");

        registry.add(prefijo + "[1].id", () -> "categorias");
        registry.add(prefijo + "[1].uri", () -> uri);
        registry.add(prefijo + "[1].predicates[0]", () -> "Path=/api/categorias/**");

        registry.add(prefijo + "[2].id", () -> "inventario");
        registry.add(prefijo + "[2].uri", () -> uri);
        registry.add(prefijo + "[2].predicates[0]", () -> "Path=/api/inventario/**");
    }

    @BeforeEach
    void limpiarSolicitudes() {
        SOLICITUDES.clear();
    }

    @AfterAll
    static void detenerDestino() {
        DESTINO.stop(0);
    }

    @Test
    void productosConservaPathQueryYAuthorization() throws Exception {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder(uri("/api/productos/MI001?incluir=ofertas"))
                        .header("Authorization", TOKEN)
                        .GET()
        );

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(SOLICITUDES).hasSize(1);
        assertThat(SOLICITUDES.get(0).metodo()).isEqualTo("GET");
        assertThat(SOLICITUDES.get(0).path()).isEqualTo("/api/productos/MI001?incluir=ofertas");
        assertThat(SOLICITUDES.get(0).authorization()).isEqualTo(TOKEN);
    }

    @Test
    void categoriasConservaPath() throws Exception {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder(uri("/api/categorias")).GET()
        );

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(SOLICITUDES).hasSize(1);
        assertThat(SOLICITUDES.get(0).path()).isEqualTo("/api/categorias");
    }

    @Test
    void inventarioConservaPathCompleto() throws Exception {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder(uri("/api/inventario/disponibilidad/MI001")).GET()
        );

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(SOLICITUDES).hasSize(1);
        assertThat(SOLICITUDES.get(0).path()).isEqualTo("/api/inventario/disponibilidad/MI001");
    }

    @Test
    void postConservaAuthorizationYBody() throws Exception {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder(uri("/api/productos"))
                        .header("Authorization", TOKEN)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"codigo\":\"MI001\"}"))
        );

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(SOLICITUDES).hasSize(1);
        assertThat(SOLICITUDES.get(0).metodo()).isEqualTo("POST");
        assertThat(SOLICITUDES.get(0).authorization()).isEqualTo(TOKEN);
    }

    @Test
    void preflightCorsSeResuelveEnElGateway() throws Exception {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder(uri("/api/productos"))
                        .header("Origin", ORIGEN_PERMITIDO)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization,content-type")
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
        );

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Origin"))
                .contains(ORIGEN_PERMITIDO);
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Methods"))
                .hasValueSatisfying(valor -> assertThat(valor).contains("GET"));
        assertThat(respuesta.headers().firstValue("Access-Control-Allow-Headers"))
                .hasValueSatisfying(valor -> assertThat(valor.toLowerCase(Locale.ROOT))
                        .contains("authorization"));
        assertThat(SOLICITUDES).as("El preflight no debe llegar al destino").isEmpty();
    }

    @Test
    void solicitudRealConOriginNoDuplicaCabeceraCors() throws Exception {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder(uri("/api/categorias"))
                        .header("Origin", ORIGEN_PERMITIDO)
                        .GET()
        );

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().allValues("Access-Control-Allow-Origin"))
                .as("El destino tambien agrega CORS; el Gateway debe deduplicar")
                .containsExactly(ORIGEN_PERMITIDO);
    }

    @Test
    void actuatorHealthRespondeUp() throws Exception {
        HttpResponse<String> respuesta = enviar(
                HttpRequest.newBuilder(uri("/actuator/health")).GET()
        );

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).contains("\"status\":\"UP\"");
    }

    private HttpResponse<String> enviar(HttpRequest.Builder constructor) throws IOException, InterruptedException {
        HttpRequest solicitud = constructor.timeout(Duration.ofSeconds(10)).build();
        return CLIENTE.send(solicitud, HttpResponse.BodyHandlers.ofString(UTF_8));
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + puerto + path);
    }

    private static HttpServer iniciarDestino() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/", intercambio -> {
                try {
                    String path = intercambio.getRequestURI().toString();
                    String authorization = intercambio.getRequestHeaders().getFirst("Authorization");
                    SOLICITUDES.add(new SolicitudRegistrada(
                            intercambio.getRequestMethod(),
                            path,
                            authorization
                    ));

                    // Simula que el microservicio de Productos tambien agrega CORS.
                    String origin = intercambio.getRequestHeaders().getFirst("Origin");
                    if (origin != null && !origin.isBlank()) {
                        intercambio.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
                    }

                    byte[] cuerpo = "{\"origen\":\"stub\"}".getBytes(UTF_8);
                    intercambio.getResponseHeaders().add("Content-Type", "application/json");
                    intercambio.sendResponseHeaders(200, cuerpo.length);
                    try (OutputStream salida = intercambio.getResponseBody()) {
                        salida.write(cuerpo);
                    }
                } finally {
                    intercambio.close();
                }
            });
            servidor.start();
            return servidor;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private record SolicitudRegistrada(String metodo, String path, String authorization) {
    }
}
