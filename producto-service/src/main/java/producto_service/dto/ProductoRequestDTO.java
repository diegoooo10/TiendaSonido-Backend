package producto_service.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Datos para crear o actualizar un producto")
public class ProductoRequestDTO {

    @Schema(description = "Codigo comercial unico del producto", example = "GA001")
    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 20, message = "El codigo no puede superar los 20 caracteres")
    @Pattern(regexp = "[A-Za-z0-9_-]+", message = "El codigo solo admite letras, numeros, guion y guion bajo")
    private String codigo;

    @Schema(description = "Nombre del producto", example = "Guitarra Acustica Folk")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
    private String nombre;

    @Schema(description = "Descripcion del producto", example = "Tapa de abeto, aros y fondo de meranti.")
    @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
    private String descripcion;

    @Schema(description = "Marca del producto", example = "Yamaha")
    @Size(max = 100, message = "La marca no puede superar los 100 caracteres")
    private String marca;

    @Schema(description = "Modelo del producto", example = "F310")
    @Size(max = 100, message = "El modelo no puede superar los 100 caracteres")
    private String modelo;

    @Schema(description = "Ruta o URL de la imagen", example = "img/guitarra.jpg")
    @Size(max = 255, message = "La imagen no puede superar los 255 caracteres")
    private String imagen;

    @Schema(description = "Precio normal de venta", example = "129990.00")
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio admite como maximo 2 decimales")
    private BigDecimal precio;

    @Schema(description = "Precio de oferta. Solo se aplica si enOferta es true", example = "109990.00")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio de oferta debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio de oferta admite como maximo 2 decimales")
    private BigDecimal precioOferta;

    @Schema(description = "Indica si el producto esta en oferta", example = "false")
    private Boolean enOferta;

    @Schema(description = "Indica si el producto esta activo y visible", example = "true")
    private Boolean activo;

    @Schema(description = "ID de la categoria asociada", example = "1")
    @NotNull(message = "Debe asignar una categoria")
    private Long categoriaId;
}
