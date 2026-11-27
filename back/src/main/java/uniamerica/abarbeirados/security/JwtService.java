package uniamerica.abarbeirados.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import uniamerica.abarbeirados.auth.Usuario;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.SignatureAlgorithm;

@Slf4j
@Service
public class JwtService {

    //Parâmetros para geração do token
    public static final SignatureAlgorithm ALGORITMO_ASSINATURA = SignatureAlgorithm.HS256;
    public static final int HORAS_EXPIRACAO_TOKEN = 1;

    private final Key signingKey;

    /*
     * A chave NÃO fica no código: vem da variável de ambiente JWT_SECRET (Base64,
     * 32 bytes ou mais). Sem ela, caso do desenvolvimento local, é gerada uma
     * chave aleatória a cada subida, então os tokens só valem até o próximo restart.
     */
    public JwtService(@Value("${jwt.secret:}") String secret) {
        if (secret == null || secret.isBlank()) {
            log.warn("JWT_SECRET não definido: usando chave aleatória, válida só até o próximo restart.");
            this.signingKey = Keys.secretKeyFor(ALGORITMO_ASSINATURA);
        } else {
            this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
    }

    public Map<String, Object> gerarPayload(Usuario usuario) {
        Map<String, Object> payloadData = new HashMap<>();
        payloadData.put("username", usuario.getUsername());
        payloadData.put("id", usuario.getId().toString());
        payloadData.put("role", usuario.getRole());

        return payloadData;
    }

    public String generateToken(Usuario usuario) {

        Map<String, Object> payloadData = this.gerarPayload(usuario);

        return Jwts
                .builder()
                .setClaims(payloadData)
                .setSubject(usuario.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(new Date().getTime() + 3600000L * HORAS_EXPIRACAO_TOKEN))
                .signWith(getSigningKey(), ALGORITMO_ASSINATURA)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Key getSigningKey() {
        return signingKey;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

}
