package tienda_sonido.usuario_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import tienda_sonido.usuario_service.model.Rol;
import tienda_sonido.usuario_service.model.Usuario;

class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-para-jwt-de-32-bytes-o-mas";

    private final SecretKey clave = new SecretKeySpec(
            SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private final JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(clave));

    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

    private final JwtService jwtService = new JwtService(encoder);

    private Usuario usuarioConRol(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setCorreo("vendedor@sonidovivo.cl");
        usuario.setPassword("hash");
        usuario.setRol(rol);
        usuario.setNombre("Vendedor");
        usuario.setApellido("Prueba");
        usuario.setRun("12345678-9");
        usuario.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        usuario.setDireccion("Calle 1");
        return usuario;
    }

    @Test
    @DisplayName("el JWT incluye el rol VENDEDOR en el claim rol")
    void emiteRolVendedor() {
        String token = jwtService.generarToken(usuarioConRol(Rol.VENDEDOR));

        Jwt jwt = decoder.decode(token);

        assertEquals("VENDEDOR", jwt.getClaimAsString("rol"));
        assertEquals("vendedor@sonidovivo.cl", jwt.getSubject());
    }

    @Test
    @DisplayName("ADMIN y CLIENTE mantienen su claim rol")
    void mantieneRolesAdminYCliente() {
        assertEquals("ADMIN", decoder.decode(
                jwtService.generarToken(usuarioConRol(Rol.ADMIN))).getClaimAsString("rol"));
        assertEquals("CLIENTE", decoder.decode(
                jwtService.generarToken(usuarioConRol(Rol.CLIENTE))).getClaimAsString("rol"));
    }
}
