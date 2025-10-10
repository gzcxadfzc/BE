package com.pkg.configs;

import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

@Configuration
public class AlgorithmConfig {

	@Bean
	RSAKeyFactory rsaKeyFactory() {
		return new RSAKeyFactory();
	}

	@Bean
	public Algorithm jwtPrivateKeyAlgorithm(
		@Value("${auth.jwt.crypto.private-key}") String privateKeyString,
		RSAKeyFactory rsaKeyFactory
	) throws InvalidKeySpecException {
		RSAPrivateKey privateKey = rsaKeyFactory.createPrivateKey(privateKeyString);
		return Algorithm.RSA256(privateKey);
	}

	@Bean
	public Algorithm jwtPublicKeyAlgorithm(
		@Value("${auth.jwt.crypto.public-key}") String publicKeyString,
		RSAKeyFactory rsaKeyFactory
	) throws InvalidKeySpecException {
		RSAPublicKey publicKey = rsaKeyFactory.createPublicKey(publicKeyString);
		return Algorithm.RSA256(publicKey);
	}
}
