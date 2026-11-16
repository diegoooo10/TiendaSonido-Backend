package producto_service.config;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogoProducto(
        String codigo,
        String categoria,
        String nombre,
        String marca,
        String modelo,
        Integer stock,
        BigDecimal precio,
        String descripcion,
        String imagen
) {
}
