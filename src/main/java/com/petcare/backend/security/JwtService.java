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

	private static final String TOKEN_TYPE_CLAIM = "token_type";
	private static final String ACCESS_TOKEN_TYPE = "access";
	private static final String REFRESH_TOKEN_TYPE = "refresh";

	private final JwtProperties properties;
	private final SecretKey signingKey;

	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
	}

	public String generateToken(String subject) {
		return generateAccessToken(subject);
	}

	public String generateAccessToken(String subject) {
		return generateToken(subject, ACCESS_TOKEN_TYPE, properties.accessExpirationMs());
	}

	public String generateRefreshToken(String subject) {
		return generateToken(subject, REFRESH_TOKEN_TYPE, properties.refreshExpirationMs());
	}

	private String generateToken(String subject, String tokenType, long expirationMs) {
		Instant now = Instant.now();

		return Jwts.builder()
				.subject(subject)
				.claim(TOKEN_TYPE_CLAIM, tokenType)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMs)))
				.signWith(signingKey)
				.compact();
	}

	public String extractSubject(String token) {
		return extractClaims(token).getSubject();
	}

	public boolean isValid(String token) {
		return isValidAccessToken(token);
	}

	public boolean isValidAccessToken(String token) {
		return isValidTokenType(token, ACCESS_TOKEN_TYPE);
	}

	public boolean isValidRefreshToken(String token) {
		return isValidTokenType(token, REFRESH_TOKEN_TYPE);
	}

	private boolean isValidTokenType(String token, String expectedType) {
		try {
			Claims claims = extractClaims(token);
			return claims.getExpiration().after(new Date())
					&& expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
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
