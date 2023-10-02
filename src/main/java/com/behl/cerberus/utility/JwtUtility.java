package com.behl.cerberus.utility;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.behl.cerberus.configuration.JwtConfigurationProperties;
import com.behl.cerberus.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtConfigurationProperties.class)
public class JwtUtility {

	private final JwtConfigurationProperties jwtConfigurationProperties;
	
	private static final String BEARER_PREFIX = "Bearer ";
	private static final String SCOPE_CLAIM_NAME = "scp";

	@Value("${spring.application.name}")
	private String issuer;

	public UUID extractUserId(@NonNull final String token) {
		final var audience = extractClaim(token, Claims::getAudience);
		return UUID.fromString(audience);
	}
	
	public String generateAccessToken(final User user) {
		final var accessTokenValidity = jwtConfigurationProperties.getJwt().getAccessToken().getValidity();
		final var expiration = TimeUnit.MINUTES.toMillis(accessTokenValidity);
		final var secretKey = jwtConfigurationProperties.getJwt().getSecretKey();
		final var currentTimestamp = new Date(System.currentTimeMillis());
		final var expirationTimestamp = new Date(System.currentTimeMillis() + expiration);
		final var scopes = user.getUserStatus().getScopes().stream().collect(Collectors.joining(StringUtils.SPACE));
		
		final var claims = new DefaultClaims();
		claims.put(SCOPE_CLAIM_NAME, scopes);
		
		return Jwts.builder()
				.setClaims(claims)
				.setIssuer(issuer)
				.setIssuedAt(currentTimestamp)
				.setExpiration(expirationTimestamp)
				.setAudience(String.valueOf(user.getId()))
				.signWith(SignatureAlgorithm.HS256, secretKey).compact();
	}
	
	public List<GrantedAuthority> getAuthority(@NonNull final String token){
		final var scopes = extractClaim(token, claims -> claims.get(SCOPE_CLAIM_NAME, String.class));
		return Arrays.stream(scopes.split(StringUtils.SPACE))
					.map(SimpleGrantedAuthority::new)
					.collect(Collectors.toList());
	}

	public Boolean validateToken(@NonNull final String token, @NonNull final UUID userId) {
		final var audience = extractClaim(token, Claims::getAudience);
		return UUID.fromString(audience).equals(userId) && !isTokenExpired(token);
	}

	public Boolean isTokenExpired(@NonNull final String token) {
		final var tokenExpirationDate = extractClaim(token, Claims::getExpiration);
		return tokenExpirationDate.before(new Date(System.currentTimeMillis()));
	}
	
	public LocalDateTime getExpirationTimestamp(@NonNull final String token) {
		final var expiration = extractClaim(token, Claims::getExpiration);
		return expiration.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
	}

	private <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
		final var secretKey = jwtConfigurationProperties.getJwt().getSecretKey();
		final var santizedToken = token.replace(BEARER_PREFIX, StringUtils.EMPTY);
		final var claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(santizedToken).getBody();
		return claimsResolver.apply(claims);
	}

}