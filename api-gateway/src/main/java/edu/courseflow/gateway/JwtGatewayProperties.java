package edu.courseflow.gateway;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "training.jwt")
public record JwtGatewayProperties(String secret, String issuer) {

    public SecretKey secretKey() {
        byte[] bytes = secret.getBytes();
        if (bytes.length < 32) {
            throw new IllegalStateException("training.jwt.secret must be at least 32 bytes for HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
