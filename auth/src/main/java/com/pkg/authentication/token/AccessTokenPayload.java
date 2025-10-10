package com.pkg.authentication.token;

public record AccessTokenPayload(
	Long memberNo,
	String role
) {
}
