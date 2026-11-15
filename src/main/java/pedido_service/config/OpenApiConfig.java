package pedido_service.config;

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
                        .title("Pedido Service API - Tienda Sonido Vivo")
                        .version("1.0.0")
                        .description("""
                                Microservicio encargado de gestionar los pedidos
                                y compras de los clientes de Tienda Sonido Vivo.

                                Permite crear pedidos, consultar el historial
                                de compras y obtener el detalle de cada pedido.

                                Los endpoints requieren autenticacion mediante
                                JWT emitido por usuario-service.
                                """)
                        .contact(new Contact()
                                .name("Equipo Tienda Sonido Vivo"))
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8084")
                                .description("Entorno local")
                ))
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(SECURITY_SCHEME)
                )
                .schemaRequirement(
                        SECURITY_SCHEME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        Token JWT emitido por usuario-service.
                                        Algoritmo: HS256.
                                        Claim de rol: rol.
                                        El subject corresponde al correo del usuario.
                                        """)
                );
    }
}
