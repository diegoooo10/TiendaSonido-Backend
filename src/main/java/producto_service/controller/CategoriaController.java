package producto_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import producto_service.dto.CategoriaRequestDTO;
import producto_service.dto.CategoriaResponseDTO;
import producto_service.service.CategoriaService;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Categorias", description = "Categorias del catalogo")
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @Operation(summary = "Listar categorias")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listar() {
        List<CategoriaResponseDTO> categorias = categoriaService.listar();
        categorias.forEach(this::agregarLinks);
        return ResponseEntity.ok(categorias);
    }

    @Operation(
            summary = "Listar todas las categorias (administracion)",
            description = "Requiere rol ADMIN. Incluye categorias activas e inactivas."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos")
    })
    @GetMapping("/admin")
    public ResponseEntity<List<CategoriaResponseDTO>> listarAdmin() {
        List<CategoriaResponseDTO> categorias = categoriaService.listarAdmin();
        categorias.forEach(this::agregarLinksAdmin);
        return ResponseEntity.ok(categorias);
    }

    @Operation(summary = "Obtener una categoria por ID")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria encontrada"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> obtener(@PathVariable Long id) {
        CategoriaResponseDTO categoria = categoriaService.obtenerPorId(id);
        agregarLinks(categoria);
        return ResponseEntity.ok(categoria);
    }

    @Operation(summary = "Crear una categoria", description = "Requiere rol ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria creada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> crear(@Valid @RequestBody CategoriaRequestDTO dto) {
        CategoriaResponseDTO categoria = categoriaService.guardar(dto);
        agregarLinks(categoria);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoria);
    }

    @Operation(
            summary = "Actualizar una categoria",
            description = "Requiere rol ADMIN. No permite desactivar una categoria con productos activos (409)."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categoria actualizada"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada"),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequestDTO dto
    ) {
        CategoriaResponseDTO categoria = categoriaService.actualizar(id, dto);
        agregarLinks(categoria);
        return ResponseEntity.ok(categoria);
    }

    @Operation(summary = "Eliminar una categoria", description = "Solo si no tiene productos asociados.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria eliminada"),
            @ApiResponse(responseCode = "404", description = "Categoria no encontrada"),
            @ApiResponse(responseCode = "409", description = "Categoria con productos asociados")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private void agregarLinksAdmin(CategoriaResponseDTO categoria) {
        categoria.add(
                linkTo(methodOn(CategoriaController.class).obtener(categoria.getId())).withSelfRel()
        );
        categoria.add(
                linkTo(methodOn(CategoriaController.class).listarAdmin()).withRel("categorias-admin")
        );
        categoria.add(
                linkTo(methodOn(CategoriaController.class).listar()).withRel("categorias")
        );
        categoria.add(
                linkTo(methodOn(ProductoController.class).buscarPorCategoria(categoria.getId()))
                        .withRel("productos")
        );
    }

    private void agregarLinks(CategoriaResponseDTO categoria) {
        categoria.add(
                linkTo(methodOn(CategoriaController.class).obtener(categoria.getId())).withSelfRel()
        );
        categoria.add(
                linkTo(methodOn(CategoriaController.class).listar()).withRel("categorias")
        );
        categoria.add(
                linkTo(methodOn(ProductoController.class).buscarPorCategoria(categoria.getId()))
                        .withRel("productos")
        );
    }
}
