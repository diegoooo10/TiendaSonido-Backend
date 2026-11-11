package producto_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "inventario")
@Getter
@Setter
public class InventarioProperties {

    /**
     * Umbral de stock critico asignado a inventarios nuevos. Es un valor de
     * configuracion, no una cantidad de stock real.
     */
    private int stockCriticoInicial = 3;
}
