package producto_service.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Producto Service API - Tienda Sonido Vivo")
                        .version("1.0.0")
                        .description("""
                                Microservicio unificado de productos, categorias, precios,
                                ofertas, inventario y reservas de stock.

                                Los endpoints publicos de catalogo no requieren token.
                                Las operaciones de gestion aceptan los roles ADMINISTRADOR y
                                VENDEDOR, ademas de la compatibilidad temporal con ADMIN y
                                EMPLEADO mientras autenticacion-service migra sus roles.
                                """)
                        .contact(new Contact().name("Equipo Tienda Sonido Vivo"))
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8083")
                                .description("Entorno local (puerto provisional)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .schemaRequirement(
                        SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT emitido por autenticacion-service. Claim de rol: 'rol'.")
                );
    }
}
