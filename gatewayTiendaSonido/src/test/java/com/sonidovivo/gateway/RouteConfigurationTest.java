
package com.sonidovivo.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

@SpringBootTest
class RouteConfigurationTest {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void defineLasCincoRutasHaciaSusMicroservicios() {
        List<RouteDefinition> rutas = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(rutas).isNotNull();
        assertThat(rutas).hasSize(5);

        verificarRuta(rutas, "productos", "/api/productos/**",
                "http://localhost:8083");
        verificarRuta(rutas, "categorias", "/api/categorias/**",
                "http://localhost:8083");
        verificarRuta(rutas, "inventario", "/api/inventario/**",
                "http://localhost:8083");
        verificarRuta(rutas, "usuarios", "/api/v1/tienda-sonido/**",
                "http://localhost:8081");
        verificarRuta(rutas, "pedidos", "/api/pedidos/**",
                "http://localhost:8082");
    }

    @Test
    void ningunaRutaModificaElPathOriginal() {
        List<RouteDefinition> rutas = routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .block();

        assertThat(rutas).isNotNull();

        rutas.forEach(ruta -> assertThat(ruta.getFilters())
                .as("La ruta %s no debe reescribir ni recortar el path", ruta.getId())
                .noneMatch(filtro -> List.of("StripPrefix", "RewritePath", "SetPath")
                        .contains(filtro.getName())));
    }

    private void verificarRuta(
            List<RouteDefinition> rutas,
            String id,
            String path,
            String destino) {

        RouteDefinition ruta = rutas.stream()
                .filter(candidata -> id.equals(candidata.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No existe la ruta " + id));

        assertThat(ruta.getUri()).hasToString(destino);

        assertThat(ruta.getPredicates())
                .as("La ruta %s debe usar el predicado Path", id)
                .anySatisfy(predicado -> {
                    assertThat(predicado.getName()).isEqualTo("Path");
                    assertThat(predicado.getArgs()).containsValue(path);
                });
    }
}
