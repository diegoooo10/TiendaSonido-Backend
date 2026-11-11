package producto_service.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import producto_service.dto.PageResponseDTO;
import producto_service.dto.ProductoRequestDTO;
import producto_service.dto.ProductoResponseDTO;
import producto_service.service.ProductoService;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Productos", description = "Catalogo, precios y ofertas de productos")
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @Operation(summary = "Listar productos activos", description = "Endpoint publico. Admite filtros por texto, categoria, marca y oferta.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listar(
            @Parameter(description = "Texto a buscar en codigo, nombre, marca o modelo")
            @RequestParam(required = false) String q,
            @Parameter(description = "ID de categoria") @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Marca exacta") @RequestParam(required = false) String marca,
            @Parameter(description = "Filtrar solo ofertas") @RequestParam(required = false) Boolean enOferta
    ) {
        List<ProductoResponseDTO> productos = productoService.listar(q, categoriaId, marca, enOferta);
        productos.forEach(this::agregarLinks);
        return ResponseEntity.ok(productos);
    }

    @Operation(summary = "Listar productos activos con paginacion")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Pagina obtenida correctamente")
    @GetMapping("/paginado")
    public ResponseEntity<PageResponseDTO<ProductoResponseDTO>> listarPaginado(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) Boolean enOferta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ProductoResponseDTO> pagina = productoService.listarPaginado(q, categoriaId, marca, enOferta, page, size);
        pagina.getContent().forEach(this::agregarLinks);
        return ResponseEntity.ok(PageResponseDTO.from(pagina));
    }

    @Operation(summary = "Listar productos en oferta")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Ofertas obtenidas correctamente")
    @GetMapping("/ofertas")
    public ResponseEntity<List<ProductoResponseDTO>> listarOfertas() {
        List<ProductoResponseDTO> productos = productoService.listarOfertas();
        productos.forEach(this::agregarLinks);
        return ResponseEntity.ok(productos);
    }

    @Operation(summary = "Buscar productos por categoria")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Productos encontrados"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<List<ProductoResponseDTO>> buscarPorCategoria(@PathVariable Long categoriaId) {
        List<ProductoResponseDTO> productos = productoService.buscarPorCategoria(categoriaId);
        productos.forEach(this::agregarLinks);
        return ResponseEntity.ok(productos);
    }

    @Operation(summary = "Obtener detalle de un producto por codigo")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @GetMapping("/{codigo}")
    public ResponseEntity<ProductoResponseDTO> obtener(@PathVariable String codigo) {
        ProductoResponseDTO producto = productoService.obtenerPorCodigo(codigo);
        agregarLinks(producto);
        return ResponseEntity.ok(producto);
    }

    @Operation(summary = "Crear un producto", description = "Requiere rol ADMINISTRADOR o VENDEDOR. El producto nace con stock 0.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos"),
            @ApiResponse(responseCode = "409", description = "Codigo duplicado")
    })
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crear(@Valid @RequestBody ProductoRequestDTO dto) {
        ProductoResponseDTO producto = productoService.guardar(dto);
        agregarLinks(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(producto);
    }

    @Operation(summary = "Actualizar un producto por codigo", description = "Requiere rol ADMINISTRADOR o VENDEDOR.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de datos")
    })
    @PutMapping("/{codigo}")
    public ResponseEntity<ProductoResponseDTO> actualizar(
            @PathVariable String codigo,
            @Valid @RequestBody ProductoRequestDTO dto
    ) {
        ProductoResponseDTO producto = productoService.actualizar(codigo, dto);
        agregarLinks(producto);
        return ResponseEntity.ok(producto);
    }

    @Operation(summary = "Desactivar un producto", description = "Baja logica: no elimina el registro fisicamente.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto desactivado"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> eliminar(@PathVariable String codigo) {
        productoService.desactivar(codigo);
        return ResponseEntity.noContent().build();
    }

    private void agregarLinks(ProductoResponseDTO producto) {
        producto.add(
                linkTo(methodOn(ProductoController.class).obtener(producto.getCodigo())).withSelfRel()
        );
        producto.add(
                linkTo(methodOn(ProductoController.class).listar(null, null, null, null)).withRel("productos")
        );
        if (producto.getCategoriaId() != null) {
            producto.add(
                    linkTo(methodOn(CategoriaController.class).obtener(producto.getCategoriaId()))
                            .withRel("categoria")
            );
        }
        producto.add(
                linkTo(methodOn(InventarioController.class).consultarDisponibilidad(producto.getCodigo()))
                        .withRel("disponibilidad")
        );
    }
}
