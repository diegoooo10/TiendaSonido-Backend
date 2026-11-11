package tienda_sonido.usuario_service.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import tienda_sonido.usuario_service.model.Usuario;

@Service 
public class JwtService {

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    private static final Duration TOKEN_DURATION = Duration.ofHours(1);

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(TOKEN_DURATION);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(usuario.getCorreo())
            .claim("rol", usuario.getRol().name())
            .issuedAt(ahora)
            .expiresAt(expiracion)
            .build();
        
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
    
}
