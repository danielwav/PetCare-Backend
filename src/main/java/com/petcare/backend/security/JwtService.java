package com.petcare.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

	private final JwtProperties properties;
	private final SecretKey signingKey;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
	}

	public String generateToken(String subject) {
		Instant now = Instant.now();

		return Jwts.builder()
				.subject(subject)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(properties.expirationMs())))
				.signWith(signingKey)
				.compact();
	}

	public String extractSubject(String token) {
		return extractClaims(token).getSubject();
	}

	public boolean isValid(String token) {
		try {
			Claims claims = extractClaims(token);
			return claims.getExpiration().after(new Date());
		} catch (RuntimeException exception) {
			return false;
		}
	}

	private Claims extractClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
