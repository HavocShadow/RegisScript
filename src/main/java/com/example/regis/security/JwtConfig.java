package com.example.regis.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtConfig {

    @Bean
    public RSAKey rsaKey() {

        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            KeyPair keyPair =
                    generator.generateKeyPair();

            return new RSAKey.Builder(
                    (RSAPublicKey) keyPair.getPublic()
            )
                    .privateKey(keyPair.getPrivate())
                    .keyID("regis-key")
                    .build();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to generate RSA key",
                    e
            );
        }
    }

    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {

        JWKSource<SecurityContext> source =
                (selector, context) ->
                        selector.select(
                                new JWKSet(rsaKey)
                        );

        return new NimbusJwtEncoder(source);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) {

        try {

            return NimbusJwtDecoder
                    .withPublicKey(
                            rsaKey.toRSAPublicKey()
                    )
                    .build();

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to create JWT decoder",
                    e
            );
        }
    }
}
