package com.pkg.configs;

import java.time.Duration;

public class JwtConfig {

	private static final String JWT_ISSUER = "little-writer";

	private final String issuer;
	private final Duration tokenLifeTime;

	private JwtConfig(String issuer, Duration tokenLifeTime) {
		this.issuer = issuer;
		this.tokenLifeTime = tokenLifeTime;
	}

	static JwtConfig createAccessToken() {
		return new JwtConfig(JWT_ISSUER, Duration.ofDays(1));
	}

	static JwtConfig createRefreshToken() {
		return new JwtConfig(JWT_ISSUER, Duration.ofDays(30));
	}

	public String getIssuer() {
		return issuer;
	}

	public Duration getTokenLifeTime() {
		return tokenLifeTime;
	}

	public enum Claims {

		ISSUER("iss"),
		ROLE("role"),
		MEMBER_NO("memberNo"),
		MEMBER_NAME("memberName"),
		EXPIRATION("exp"),
		ISSUED_AT("iat");

		private final String name;

		Claims(String claimName) {
			this.name = claimName;
		}

		public String getName() {
			return name;
		}
	}
}
