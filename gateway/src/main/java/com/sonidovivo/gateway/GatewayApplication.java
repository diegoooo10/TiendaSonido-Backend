package com.sonidovivo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del API Gateway de Tienda Sonido Vivo.
 *
 * El Gateway es un componente reactivo (WebFlux) sin base de datos propia.
 * La autenticacion y la autorizacion siguen siendo responsabilidad de cada
 * microservicio; este componente solo enruta las solicitudes.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
