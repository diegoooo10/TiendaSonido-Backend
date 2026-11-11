package producto_service.dto;

import java.math.BigDecimal;

import org.springframework.hateoas.RepresentationModel;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de un producto")
public class ProductoResponseDTO extends RepresentationModel<ProductoResponseDTO> {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "GA001")
    private String codigo;

    @Schema(example = "Guitarra Acustica Folk")
    private String nombre;

    @Schema(example = "Tapa de abeto, aros y fondo de meranti.")
    private String descripcion;

    @Schema(example = "Yamaha")
    private String marca;

    @Schema(example = "F310")
    private String modelo;

    @Schema(example = "img/guitarra.jpg")
    private String imagen;

    @Schema(example = "129990.00")
    private BigDecimal precio;

    @Schema(example = "109990.00", nullable = true)
    private BigDecimal precioOferta;

    @Schema(example = "false")
    private Boolean enOferta;

    @Schema(description = "Precio que debe cobrarse: oferta si esta vigente, precio normal en caso contrario", example = "129990.00")
    private BigDecimal precioEfectivo;

    @Schema(example = "true")
    private Boolean activo;

    @Schema(example = "1")
    private Long categoriaId;

    @Schema(example = "Guitarras Acusticas")
    private String categoriaNombre;

    @Schema(description = "Unidades disponibles para vender (stock fisico - reservado)", example = "8")
    private Integer stockDisponible;

    @Schema(description = "Alias de compatibilidad para el frontend; equivale a stockDisponible", example = "8")
    private Integer stock;

    @Schema(description = "Indica si hay al menos una unidad disponible y el producto esta activo", example = "true")
    private Boolean disponible;
}
