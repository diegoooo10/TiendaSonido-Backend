package producto_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import producto_service.dto.ActualizarStockRequestDTO;
import producto_service.dto.DisponibilidadResponseDTO;
import producto_service.dto.InventarioResponseDTO;
import producto_service.dto.ReservaStockRequestDTO;
import producto_service.dto.ReservaStockResponseDTO;
import producto_service.service.InventarioService;
import producto_service.service.ReservaStockService;
import producto_service.service.ReservaStockService.ResultadoReserva;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Inventario", description = "Stock fisico, disponibilidad y reservas para pedidos")
@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final ReservaStockService reservaStockService;

    @Operation(summary = "Consultar inventario general", description = "Requiere rol ADMIN o EMPLEADO.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Inventario obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<InventarioResponseDTO>> listar() {
        List<InventarioResponseDTO> inventarios = inventarioService.listar();
        inventarios.forEach(this::agregarLinks);
        return ResponseEntity.ok(inventarios);
    }

    @Operation(summary = "Consultar disponibilidad publica de un producto")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad obtenida"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado o inactivo")
    })
    @GetMapping("/disponibilidad/{codigo}")
    public ResponseEntity<DisponibilidadResponseDTO> consultarDisponibilidad(@PathVariable String codigo) {
        return ResponseEntity.ok(inventarioService.consultarDisponibilidad(codigo));
    }

    @Operation(summary = "Consultar inventario por codigo de producto")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventario encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto o inventario no encontrado")
    })
    @GetMapping("/{codigo}")
    public ResponseEntity<InventarioResponseDTO> obtener(@PathVariable String codigo) {
        InventarioResponseDTO inventario = inventarioService.obtenerPorCodigo(codigo);
        agregarLinks(inventario);
        return ResponseEntity.ok(inventario);
    }

    @Operation(summary = "Actualizar stock fisico", description = "Requiere rol ADMIN o EMPLEADO. No permite dejar el stock fisico bajo el reservado.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "El stock fisico quedaria bajo el reservado")
    })
    @PutMapping("/{codigo}/stock")
    public ResponseEntity<InventarioResponseDTO> actualizarStock(
            @PathVariable String codigo,
            @Valid @RequestBody ActualizarStockRequestDTO dto
    ) {
        InventarioResponseDTO inventario = inventarioService.actualizarStock(codigo, dto);
        agregarLinks(inventario);
        return ResponseEntity.ok(inventario);
    }

    @Operation(summary = "Reservar stock para un pedido", description = "Operacion idempotente por referencia. Requiere rol ADMIN o EMPLEADO.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada"),
            @ApiResponse(responseCode = "200", description = "Reserva ya existente con la misma referencia"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente o referencia reutilizada con otros datos")
    })
    @PostMapping("/reservas")
    public ResponseEntity<ReservaStockResponseDTO> reservar(@Valid @RequestBody ReservaStockRequestDTO dto) {
        ResultadoReserva resultado = reservaStockService.reservar(dto);
        HttpStatus estado = resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(resultado.reserva());
    }

    @Operation(summary = "Consultar una reserva por referencia")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    @GetMapping("/reservas/{referencia}")
    public ResponseEntity<ReservaStockResponseDTO> obtenerReserva(@PathVariable String referencia) {
        return ResponseEntity.ok(reservaStockService.obtener(referencia));
    }

    @Operation(summary = "Listar reservas de un pedido")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Reservas del pedido")
    @GetMapping("/reservas/pedido/{pedidoId}")
    public ResponseEntity<List<ReservaStockResponseDTO>> listarReservasPorPedido(@PathVariable String pedidoId) {
        return ResponseEntity.ok(reservaStockService.listarPorPedido(pedidoId));
    }

    @Operation(summary = "Confirmar una reserva", description = "Descuenta el stock fisico y el reservado. Idempotente si ya estaba confirmada.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "Reserva liberada o inconsistencia de inventario")
    })
    @PostMapping("/reservas/{referencia}/confirmar")
    public ResponseEntity<ReservaStockResponseDTO> confirmarReserva(@PathVariable String referencia) {
        return ResponseEntity.ok(reservaStockService.confirmar(referencia));
    }

    @Operation(summary = "Liberar una reserva", description = "Devuelve las unidades reservadas sin descontar stock fisico. Idempotente si ya estaba liberada.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva liberada"),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada"),
            @ApiResponse(responseCode = "409", description = "Reserva confirmada o inconsistencia de inventario")
    })
    @PostMapping("/reservas/{referencia}/liberar")
    public ResponseEntity<ReservaStockResponseDTO> liberarReserva(@PathVariable String referencia) {
        return ResponseEntity.ok(reservaStockService.liberar(referencia));
    }

    private void agregarLinks(InventarioResponseDTO inventario) {
        inventario.add(
                linkTo(methodOn(InventarioController.class).obtener(inventario.getCodigoProducto()))
                        .withSelfRel()
        );
        inventario.add(
                linkTo(methodOn(InventarioController.class).listar()).withRel("inventario")
        );
        inventario.add(
                linkTo(methodOn(ProductoController.class).obtener(inventario.getCodigoProducto()))
                        .withRel("producto")
        );
    }
}
