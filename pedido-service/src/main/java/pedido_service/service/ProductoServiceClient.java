package pedido_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import pedido_service.dto.DisponibilidadResponseDTO;
import pedido_service.dto.ProductoResponseDTO;
import pedido_service.dto.ReservaStockRequestDTO;
import pedido_service.dto.ReservaStockResponseDTO;

@Component
public class ProductoServiceClient {

    private final RestClient restClient;

    public ProductoServiceClient(
            RestClient.Builder builder,
            @Value("${producto-service.url:http://localhost:8083}")
            String productoServiceUrl) {

        this.restClient = builder
                .baseUrl(productoServiceUrl)
                .build();
    }

    public ProductoResponseDTO obtenerProducto(String codigo) {

        return restClient.get()
                .uri("/api/productos/{codigo}", codigo)
                .retrieve()
                .body(ProductoResponseDTO.class);
    }

    public DisponibilidadResponseDTO consultarDisponibilidad(
            String codigo) {

        return restClient.get()
                .uri("/api/inventario/disponibilidad/{codigo}", codigo)
                .retrieve()
                .body(DisponibilidadResponseDTO.class);
    }

    public ReservaStockResponseDTO reservarStock(
            ReservaStockRequestDTO solicitud,
            String token) {

        return restClient.post()
                .uri("/api/inventario/reservas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(solicitud)
                .retrieve()
                .body(ReservaStockResponseDTO.class);
    }

    public ReservaStockResponseDTO confirmarReserva(
            String referencia,
            String token) {

        return restClient.post()
                .uri("/api/inventario/reservas/{referencia}/confirmar",
                        referencia)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(ReservaStockResponseDTO.class);
    }

    public ReservaStockResponseDTO liberarReserva(
            String referencia,
            String token) {

        return restClient.post()
                .uri("/api/inventario/reservas/{referencia}/liberar",
                        referencia)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(ReservaStockResponseDTO.class);
    }
}
