package com.pkg.configs;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RSAKeyFactory {

	private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
	private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
	private static final String X509_HEADER = "-----BEGIN PUBLIC KEY-----";
	private static final String X509_FOOTER = "-----END PUBLIC KEY-----";
	private static final String SPACE = "\\s";
	private static final String RSA_ALGORITHM = "RSA";

	private final KeyFactory keyFactory;

	public RSAKeyFactory() {
		try {
			keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("RSAKeyFactory initializing failed");
		}
	}

	public RSAPrivateKey createPrivateKey(String keyString) throws InvalidKeySpecException {
		String refinedKey = keyString.replace(PKCS8_HEADER, "")
			.replace(PKCS8_FOOTER, "")
			.replaceAll(SPACE, "");
		byte[] decoded = Base64.getDecoder().decode(refinedKey);
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decoded);
		return (RSAPrivateKey)keyFactory.generatePrivate(privateKeySpec);
	}

	public RSAPublicKey createPublicKey(String keyString) throws InvalidKeySpecException {
		String refinedKey = keyString.replace(X509_HEADER, "")
			.replace(X509_FOOTER, "")
			.replaceAll(SPACE, "");
		byte[] decoded = Base64.getDecoder().decode(refinedKey);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decoded);
		return (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);
	}
}
