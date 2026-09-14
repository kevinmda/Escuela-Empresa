package com.EscuelaEmpresa.gestor_pasantes.config;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Genera y valida los tokens de la API movil (/api/movil/**). El subject del token
// es el email del alumno, igual que el "username" que usa Spring Security en el
// login web -- asi CustomUserDetailsService se puede reutilizar tal cual.
@Component
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${app.jwt.secret:}") String secreto,
                       @Value("${app.jwt.expiration-ms:86400000}") long expiracionMs) {
        // igual criterio que rememberMeKey en SecurityConfig: sin clave fija, se
        // genera una al azar en el arranque (los tokens emitidos antes de reiniciar
        // el server dejan de ser validos, aceptable para desarrollo/TP).
        this.clave = secreto.isBlank()
                ? Jwts.SIG.HS256.key().build()
                : Keys.hmacShaKeyFor(secreto.getBytes());
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(String email) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave)
                .compact();
    }

    public String extraerEmail(String token) {
        return Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean esValido(String token) {
        try {
            extraerEmail(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
