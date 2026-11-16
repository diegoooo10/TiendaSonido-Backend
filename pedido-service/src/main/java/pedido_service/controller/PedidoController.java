package pedido_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pedido_service.dto.CrearPedidoRequestDTO;
import pedido_service.dto.PedidoResponseDTO;
import pedido_service.dto.PedidoResumenDTO;
import pedido_service.service.PedidoService;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Validated
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> crearPedido(
            @Valid @RequestBody CrearPedidoRequestDTO solicitud,
            @AuthenticationPrincipal Jwt jwt) {

        PedidoResponseDTO pedido = pedidoService.crearPedido(
                solicitud,
                jwt.getSubject(),
                jwt.getTokenValue()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pedido);
    }

    @GetMapping
    public ResponseEntity<List<PedidoResumenDTO>> listarPedidos(
            @AuthenticationPrincipal Jwt jwt) {

        List<PedidoResumenDTO> pedidos =
                pedidoService.listarPorUsuario(jwt.getSubject());

        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/{numeroPedido}")
    public ResponseEntity<PedidoResponseDTO> obtenerPedido(
            @PathVariable String numeroPedido,
            @AuthenticationPrincipal Jwt jwt) {

        PedidoResponseDTO pedido = pedidoService.obtenerPorNumero(
                numeroPedido,
                jwt.getSubject()
        );

        return ResponseEntity.ok(pedido);
    }

    @PostMapping("/{numeroPedido}/reintentar-confirmacion")
    public ResponseEntity<PedidoResponseDTO> reintentarConfirmacion(
            @PathVariable String numeroPedido,
            @AuthenticationPrincipal Jwt jwt) {

        PedidoResponseDTO pedido = pedidoService.reintentarConfirmacion(
                numeroPedido,
                jwt.getSubject(),
                jwt.getTokenValue()
        );

        return ResponseEntity.ok(pedido);
    }

    @DeleteMapping("/{numeroPedido}")
    public ResponseEntity<PedidoResponseDTO> cancelarPedido(
            @PathVariable String numeroPedido,
            @AuthenticationPrincipal Jwt jwt) {

        PedidoResponseDTO pedido = pedidoService.cancelarPedido(
                numeroPedido,
                jwt.getSubject(),
                jwt.getTokenValue()
        );

        return ResponseEntity.ok(pedido);
    }
}
